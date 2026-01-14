package br.com.infnet.itinventory.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "profile")
@Getter
@Setter
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_profile")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code; // ADMIN, GESTOR_TI, ANALISTA_TI, USUARIO

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "nivel_acesso", nullable = false)
    private Integer nivelAcesso; // 0-3 (conforme sua regra)

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;
}