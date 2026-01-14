package br.com.infnet.itinventory.search.service;

import br.com.infnet.itinventory.model.Equipment;
import br.com.infnet.itinventory.search.doc.EquipmentDoc;
import br.com.infnet.itinventory.search.dto.EquipmentSearchRequest;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import co.elastic.clients.elasticsearch._types.Refresh;
import java.io.IOException;
import java.util.Objects;


@ConditionalOnProperty(prefix = "search.es", name = "enabled", havingValue = "true")
@Service
@RequiredArgsConstructor
public class EquipmentSearchService {


    @Value("${search.es.index:itinventory-equipments}")
    private String indexName;

    private final ElasticsearchClient esClient;

    /**
     * Busca simples por texto (full-text), com relevância e fuzziness.
     * Ideal para campo de busca do front.
     */
    public SearchResponse<EquipmentDoc> buscarPorTextoRaw(String texto, int page, int size) throws IOException {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int from = safePage * safeSize;

        return esClient.search(s -> s
                        .index(indexName)
                        .from(from)
                        .size(safeSize)
                        .trackScores(true)
                        .query(q -> q
                                .multiMatch(m -> m
                                        .query(texto)
                                        .fields(
                                                "assetNumber^3",
                                                "brand^2",
                                                "model^2",
                                                "responsible",
                                                "location",
                                                "description"
                                        )
                                        .fuzziness("AUTO")
                                )
                        )
                        .highlight(h -> h
                                .preTags("<mark>")
                                .postTags("</mark>")
                                .fields("description", hf -> hf)
                        )
                        .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                EquipmentDoc.class
        );
    }

    /**
     * Conveniência: retorna somente a lista de docs.
     */
    public java.util.List<EquipmentDoc> buscarPorTexto(String texto, int page, int size) throws IOException {
        SearchResponse<EquipmentDoc> response = buscarPorTextoRaw(texto, page, size);
        return response.hits().hits().stream()
                .map(h -> h.source())
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Busca avançada: combina texto + filtros opcionais (status, type, location) + range (valor, data).
     */
    public java.util.List<EquipmentDoc> buscaAvancada(EquipmentSearchRequest req, int page, int size) throws IOException {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int from = safePage * safeSize;

        SearchResponse<EquipmentDoc> response = esClient.search(s -> s
                        .index(indexName)
                        .from(from)
                        .size(safeSize)
                        .trackScores(true)
                        .query(q -> q.bool(b -> {

                            // Texto livre (opcional)
                            if (req != null && req.texto() != null && !req.texto().isBlank()) {
                                b.must(m -> m.multiMatch(mm -> mm
                                        .query(req.texto())
                                        .fields(
                                                "assetNumber^3",
                                                "brand^2",
                                                "model^2",
                                                "responsible",
                                                "location",
                                                "description"
                                        )
                                        .fuzziness("AUTO")
                                ));
                            }

                            // Filtros por termo (idealmente campos keyword no mapping)
                            if (req != null && req.status() != null && !req.status().isBlank()) {
                                b.filter(f -> f.term(t -> t.field("status").value(req.status())));
                            }
                            if (req != null && req.type() != null && !req.type().isBlank()) {
                                b.filter(f -> f.term(t -> t.field("type").value(req.type())));
                            }
                            if (req != null && req.location() != null && !req.location().isBlank()) {
                                b.filter(f -> f.term(t -> t.field("location").value(req.location())));
                            }

                            // Range de valor (opcional)
                            if (req != null && (req.minValue() != null || req.maxValue() != null)) {
                                b.filter(f -> f.range(r -> r.number(n -> {
                                    n.field("acquisitionValue");
                                    if (req.minValue() != null) n.gte(req.minValue());
                                    if (req.maxValue() != null) n.lte(req.maxValue());
                                    return n;
                                })));
                            }

                            // Range de data (opcional)
                            if (req != null && (
                                    (req.dateFrom() != null && !req.dateFrom().isBlank()) ||
                                            (req.dateTo() != null && !req.dateTo().isBlank())
                            )) {
                                b.filter(f -> f.range(r -> r.date(d -> {
                                    d.field("acquisitionDate");
                                    if (req.dateFrom() != null && !req.dateFrom().isBlank()) d.gte(req.dateFrom());
                                    if (req.dateTo() != null && !req.dateTo().isBlank()) d.lte(req.dateTo());
                                    return d;
                                })));
                            }

                            return b;
                        }))
                        .highlight(h -> h
                                .preTags("<mark>")
                                .postTags("</mark>")
                                .fields("description", hf -> hf)
                        )
                        .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                EquipmentDoc.class
        );

        return response.hits().hits().stream()
                .map(h -> h.source())
                .filter(Objects::nonNull)
                .toList();
    }
    public void upsert(Equipment e) {
        try {
            EquipmentDoc doc = toDoc(e);

            esClient.index(i -> i
                    .index(indexName)
                    .id(String.valueOf(e.getId()))     // _id = ID do MySQL
                    .document(doc)
                    .refresh(Refresh.WaitFor)          // opcional, ajuda no “near real-time” do teste
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao indexar equipamento no Elasticsearch. id=" + e.getId(), ex);
        }
    }

    public void deleteById(Long id) {
        try {
            esClient.delete(d -> d
                    .index(indexName)
                    .id(String.valueOf(id))
                    .refresh(Refresh.WaitFor)
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao remover equipamento no Elasticsearch. id=" + id, ex);
        }
    }
    private EquipmentDoc toDoc(Equipment e) {
        return EquipmentDoc.builder()
                .idEquipment(e.getId())
                .assetNumber(e.getAssetNumber())
                .type(e.getType() != null ? e.getType().name() : null)
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .brand(e.getBrand())
                .model(e.getModel())
                .location(e.getLocation())
                .responsible(e.getResponsible())
                .acquisitionDate(e.getAcquisitionDate())
                .acquisitionValue(e.getAcquisitionValue() != null ? e.getAcquisitionValue().doubleValue() : null)
                .description(buildDescription(e))
                .build();
    }

    private String buildDescription(Equipment e) {
        // campo "unificado" para multi_match / highlight
        StringBuilder sb = new StringBuilder();
        if (e.getAssetNumber() != null) sb.append(e.getAssetNumber()).append(" ");
        if (e.getBrand() != null) sb.append(e.getBrand()).append(" ");
        if (e.getModel() != null) sb.append(e.getModel()).append(" ");
        if (e.getResponsible() != null) sb.append(e.getResponsible()).append(" ");
        if (e.getLocation() != null) sb.append(e.getLocation()).append(" ");
        return sb.toString().trim();
    }


}
