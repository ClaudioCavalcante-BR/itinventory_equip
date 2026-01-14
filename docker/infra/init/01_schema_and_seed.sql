-- ==========================================
-- 0. CRIAR E USAR DATABASE
-- ==========================================
CREATE DATABASE IF NOT EXISTS itinventory_equip
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE itinventory_equip;



-- ==========================================
-- 1 - Tabela: profile
-- Criar tabela profile
-- ==========================================

CREATE TABLE IF NOT EXISTS `profile` (
    id_profile    BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,   -- 'ADMIN','GESTOR_TI','ANALISTA_TI','USUARIO'
    name          VARCHAR(100) NOT NULL,          -- nome amigável
    descricao     VARCHAR(255) NULL,

    -- nível de acesso genérico (0 a 3)
    -- 3 = acesso total (CRUD)
    -- 2 = criar/atualizar/ler
    -- 1 = somente leitura
    -- 0 = sem acesso
    nivel_acesso  TINYINT NOT NULL,

    ativo         BOOLEAN NOT NULL DEFAULT 1,
    criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP

) ENGINE=InnoDB;


-- ==========================================
-- 1.1 - Popular perfis padrão com níveis de acesso
-- ==========================================

INSERT INTO `profile` (code, name, descricao, nivel_acesso)
VALUES
    ('ADMIN', 'Administrador do Sistema', 'Acesso total ao inventário de equipamentos', 3),
    ('GESTOR_TI', 'Gestor de TI', 'Gestor de TI com permissão ampla sobre equipamentos', 3),
    ('ANALISTA_TI','Analista de Suporte de TI', 'Pode criar e atualizar equipamentos, mas não excluir', 2),
    ('USUARIO',  'Usuário Operacional', 'Pode apenas consultar o inventário',  1);



-- ==========================================
-- 2. TABELA DE USUÁRIOS
--    Nome: users
-- ==========================================
-- Campos:
--  id_user (PK)
--  name, job_title, email, dominio, password, profile
--  ativo, criado_em, atualizado_em

CREATE TABLE IF NOT EXISTS users (

    id_user       BIGINT AUTO_INCREMENT PRIMARY KEY,

    name          VARCHAR(150) NOT NULL,
    job_title     VARCHAR(150) NOT NULL,          -- TEXTO amigável (ex.: "Administrador do Sistema")
    email         VARCHAR(150) NOT NULL UNIQUE,
    dominio       VARCHAR(255) NULL,        -- Ex.: URL do avatar
    password      VARCHAR(255) NOT NULL,    -- Idealmente hash (BCrypt etc.)

    id_profile    BIGINT NOT NULL,                -- FK obrigatória -> profile(id_profile)

    ativo         BOOLEAN NOT NULL DEFAULT 1,
    criado_em     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_users_profile
        FOREIGN KEY (id_profile)
        REFERENCES profile (id_profile)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
    ) ENGINE=InnoDB;

    CREATE INDEX idx_users_id_profile ON users (id_profile);



-- ==========================================
-- 3. TABELA DE ENDEREÇOS
--    Nome: endereco
-- ==========================================
-- Campos:
--  id_endereco (PK)
--  logradouro, numero, complemento, bairro
--  cidade, estado, cep, pais

CREATE TABLE IF NOT EXISTS endereco (
    id_endereco   BIGINT AUTO_INCREMENT PRIMARY KEY,
    logradouro    VARCHAR(200) NOT NULL,
    numero        VARCHAR(20)  NULL,
    complemento   VARCHAR(100) NULL,
    bairro        VARCHAR(100) NULL,
    cidade        VARCHAR(100) NOT NULL,
    estado        CHAR(2)      NOT NULL,
    cep           VARCHAR(15)  NOT NULL,
    pais          VARCHAR(100) NOT NULL DEFAULT 'Brasil'
) ENGINE=InnoDB;

CREATE INDEX idx_endereco_cidade_estado
    ON endereco (cidade, estado);

-- ==========================================
-- 4. TABELA DE FILIAIS / UNIDADES
--    Nome: filial
-- ==========================================
-- Campos:
--  id_filial (PK)
--  nome, cnpj, tipo
--  id_endereco (FK -> endereco)
--  ativo, criado_em, atualizado_em

CREATE TABLE IF NOT EXISTS filial (
    id_filial     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(150) NOT NULL,
    cnpj          VARCHAR(18)  NULL,
    tipo          ENUM('MATRIZ','FILIAL','BASE_OPERACIONAL')
                  NOT NULL DEFAULT 'FILIAL',
    id_endereco   BIGINT       NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT 1,
    criado_em     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_filial_endereco
        FOREIGN KEY (id_endereco)
        REFERENCES endereco (id_endereco)
        ON UPDATE CASCADE
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_filial_ativo ON filial (ativo);


-- ==========================================
-- 5. TABELA DE FORNECEDORES
--    Nome: fornecedor
-- ==========================================
-- Campos:
--  id_fornecedor (PK)
--  razao_social, nome_fantasia, cnpj
--  email_contato, telefone_contato
--  id_endereco (FK -> endereco)
--  ativo, criado_em, atualizado_em

CREATE TABLE IF NOT EXISTS fornecedor (
    id_fornecedor     BIGINT AUTO_INCREMENT PRIMARY KEY,
    razao_social      VARCHAR(150) NOT NULL,
    nome_fantasia     VARCHAR(150) NULL,
    cnpj              VARCHAR(20)  NULL,
    email_contato     VARCHAR(150) NULL,
    telefone_contato  VARCHAR(50)  NULL,
    id_endereco       BIGINT       NULL,
    ativo             BOOLEAN      NOT NULL DEFAULT 1,
    criado_em         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_fornecedor_endereco
        FOREIGN KEY (id_endereco)
        REFERENCES endereco (id_endereco)
        ON UPDATE CASCADE
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_fornecedor_ativo ON fornecedor (ativo);

-- ==========================================
-- 6. TABELA DE CATEGORIA DE ATIVO
--    Nome: categoria_ativo
-- ==========================================
-- Campos:
--  id_categoria (PK)
--  nome, descricao
--  vida_util_anos_padrao, ativo

CREATE TABLE IF NOT EXISTS categoria_ativo (
    id_categoria          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                  VARCHAR(100) NOT NULL,
    descricao             TEXT        NULL,
    vida_util_anos_padrao INT         NULL,
    ativo                 BOOLEAN     NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE UNIQUE INDEX uq_categoria_ativo_nome
    ON categoria_ativo (nome);


-- ==========================================
-- 7. TABELA DE EQUIPAMENTOS
--    Nome: equipment
-- ==========================================
-- Campos:
--  id_equipment (PK)
--  type, brand, model, asset_number
--  status, location, responsible
--  acquisition_date, acquisition_value
--  ativo, criado_em, atualizado_em
--  id_categoria (FK -> categoria_ativo)
--  id_filial_atual (FK -> filial)
--  id_fornecedor (FK -> fornecedor)

CREATE TABLE IF NOT EXISTS equipment (
    id_equipment       BIGINT AUTO_INCREMENT PRIMARY KEY,

    `type`             ENUM('NOTEBOOK','DESKTOP','MONITOR','SERVIDOR','IMPRESSORA','ROTEADOR','SWITCH','SMARTPHONE')
                       NOT NULL,
    brand              VARCHAR(100) NOT NULL,
    model              VARCHAR(150) NOT NULL,
    asset_number       CHAR(9)  NOT NULL UNIQUE,

    status             ENUM('EM_USO','EM_MANUTENCAO','EM_ESTOQUE','DESCARTADO','RESERVADO','EM_GARANTIA','AGUARDANDO_DESCARTE','PERDIDO_OU_ROUBADO')
                       NOT NULL,

    location           VARCHAR(150) NOT NULL,
    responsible        VARCHAR(150) NOT NULL,

    acquisition_date   DATE         NOT NULL,
    acquisition_value  DECIMAL(15,2) NOT NULL,

    ativo              BOOLEAN      NOT NULL DEFAULT 1,
    criado_em          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em      DATETIME     NULL ON UPDATE CURRENT_TIMESTAMP,

    id_categoria       BIGINT       NULL,
    id_filial_atual    BIGINT       NULL,
    id_fornecedor      BIGINT       NULL,

    CONSTRAINT fk_equipment_categoria
        FOREIGN KEY (id_categoria)
        REFERENCES categoria_ativo (id_categoria)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_equipment_filial
        FOREIGN KEY (id_filial_atual)
        REFERENCES filial (id_filial)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_equipment_fornecedor
        FOREIGN KEY (id_fornecedor)
        REFERENCES fornecedor (id_fornecedor)
        ON UPDATE CASCADE
        ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_equipment_status ON equipment (status);
CREATE INDEX idx_equipment_type   ON equipment (`type`);
CREATE INDEX idx_equipment_loc    ON equipment (location);


-- ==========================================
-- 8 - INSERTS: users
--    Ajuste feito:
--    - job_title agora respeita o ENUM (ADMIN/GESTOR_TI/ANALISTA_TI/USUARIO)
--    - id_profile referencia os ids gerados no INSERT de profile (1..4)
-- ==========================================

INSERT INTO users (
                   name,
                   job_title,
                   email,
                   dominio,
                   password,
                   id_profile,
                   ativo
) VALUES
('Paulo Henrique Martins',      'Administrador do Sistema', 'paulo.martins051@empresa.com',      'https://avatar.empresa.com/user051.png', '123456', 1, 1),
('Aline Rodrigues Moreira',     'Gestor de TI',             'aline.moreira052@empresa.com',      'https://avatar.empresa.com/user052.png', '123456', 2, 1),
('Rafael Augusto Ferreira',     'Analista de Suporte de TI','rafael.ferreira053@empresa.com',    'https://avatar.empresa.com/user053.png', '123456', 3, 1),
('Camila Nascimento Lima',      'Usuário Operacional',      'camila.lima054@empresa.com',        'https://avatar.empresa.com/user054.png', '123456', 4, 1),
('Bruno Vinicius Almeida',      'Administrador do Sistema', 'bruno.almeida055@empresa.com',      'https://avatar.empresa.com/user055.png', '123456', 1, 1),
('Juliana Pereira Santos',      'Gestor de TI',             'juliana.santos056@empresa.com',     'https://avatar.empresa.com/user056.png', '123456', 2, 1),
('Thiago Carvalho Rocha',       'Analista de Suporte de TI','thiago.rocha057@empresa.com',       'https://avatar.empresa.com/user057.png', '123456', 3, 1),
('Mariana Silva Campos',        'Usuário Operacional',      'mariana.campos058@empresa.com',     'https://avatar.empresa.com/user058.png', '123456', 4, 1),
('Diego Henrique Araujo',       'Administrador do Sistema', 'diego.araujo059@empresa.com',       'https://avatar.empresa.com/user059.png', '123456', 1, 1),
('Patricia Oliveira Costa',     'Gestor de TI',             'patricia.costa060@empresa.com',     'https://avatar.empresa.com/user060.png', '123456', 2, 1),
('Guilherme Souza Ribeiro',     'Analista de Suporte de TI','guilherme.ribeiro061@empresa.com',  'https://avatar.empresa.com/user061.png', '123456', 3, 1),
('Larissa Barbosa Freitas',     'Usuário Operacional',      'larissa.freitas062@empresa.com',    'https://avatar.empresa.com/user062.png', '123456', 4, 1),
('Rodrigo Teixeira Mendes',     'Administrador do Sistema', 'rodrigo.mendes063@empresa.com',     'https://avatar.empresa.com/user063.png', '123456', 1, 1),
('Fernanda Gomes Cardoso',      'Gestor de TI',             'fernanda.cardoso064@empresa.com',   'https://avatar.empresa.com/user064.png', '123456', 2, 1),
('Felipe Castro Pereira',       'Analista de Suporte de TI','felipe.pereira065@empresa.com',     'https://avatar.empresa.com/user065.png', '123456', 3, 1),
('Bianca Martins Vieira',       'Usuário Operacional',      'bianca.vieira066@empresa.com',      'https://avatar.empresa.com/user066.png', '123456', 4, 1),
('Eduardo Lima Fernandes',      'Administrador do Sistema', 'eduardo.fernandes067@empresa.com',  'https://avatar.empresa.com/user067.png', '123456', 1, 1),
('Beatriz Rocha Alves',         'Gestor de TI',             'beatriz.alves068@empresa.com',      'https://avatar.empresa.com/user068.png', '123456', 2, 1),
('Vinicius Ribeiro Souza',      'Analista de Suporte de TI','vinicius.souza069@empresa.com',     'https://avatar.empresa.com/user069.png', '123456', 3, 1),
('Carolina Mendes Araujo',      'Usuário Operacional',      'carolina.araujo070@empresa.com',    'https://avatar.empresa.com/user070.png', '123456', 4, 1),
('Gabriel Fernandes Lima',      'Administrador do Sistema', 'gabriel.lima071@empresa.com',       'https://avatar.empresa.com/user071.png', '123456', 1, 1),
('Renata Carvalho Santos',      'Gestor de TI',             'renata.santos072@empresa.com',      'https://avatar.empresa.com/user072.png', '123456', 2, 1),
('Lucas Moreira Oliveira',      'Analista de Suporte de TI','lucas.oliveira073@empresa.com',     'https://avatar.empresa.com/user073.png', '123456', 3, 1),
('Isabela Costa Pereira',       'Usuário Operacional',      'isabela.pereira074@empresa.com',    'https://avatar.empresa.com/user074.png', '123456', 4, 1),
('Anderson Silva Rocha',        'Administrador do Sistema', 'anderson.rocha075@empresa.com',     'https://avatar.empresa.com/user075.png', '123456', 1, 1),
('Tatiane Souza Almeida',       'Gestor de TI',             'tatiane.almeida076@empresa.com',    'https://avatar.empresa.com/user076.png', '123456', 2, 1),
('Leonardo Barros Ferreira',    'Analista de Suporte de TI','leonardo.ferreira077@empresa.com',  'https://avatar.empresa.com/user077.png', '123456', 3, 1),
('Priscila Nascimento Gomes',   'Usuário Operacional',      'priscila.gomes078@empresa.com',     'https://avatar.empresa.com/user078.png', '123456', 4, 1),
('Matheus Cardoso Ribeiro',     'Administrador do Sistema', 'matheus.ribeiro079@empresa.com',    'https://avatar.empresa.com/user079.png', '123456', 1, 1),
('Amanda Vieira Santos',        'Gestor de TI',             'amanda.santos080@empresa.com',      'https://avatar.empresa.com/user080.png', '123456', 2, 1),
('Joao Vitor Lima Costa',       'Analista de Suporte de TI','joaovitor.costa081@empresa.com',    'https://avatar.empresa.com/user081.png', '123456', 3, 1),
('Bruna Fernandes Araujo',      'Usuário Operacional',      'bruna.araujo082@empresa.com',       'https://avatar.empresa.com/user082.png', '123456', 4, 1),
('Ricardo Alves Moreira',       'Administrador do Sistema', 'ricardo.moreira083@empresa.com',    'https://avatar.empresa.com/user083.png', '123456', 1, 1),
('Vanessa Oliveira Rocha',      'Gestor de TI',             'vanessa.rocha084@empresa.com',      'https://avatar.empresa.com/user084.png', '123456', 2, 1),
('Marcos Vinicius Santos',      'Analista de Suporte de TI','marcos.santos085@empresa.com',      'https://avatar.empresa.com/user085.png', '123456', 3, 1),
('Jéssica Pereira Lima',        'Usuário Operacional',      'jessica.lima086@empresa.com',       'https://avatar.empresa.com/user086.png', '123456', 4, 1),
('Henrique Souza Carvalho',     'Administrador do Sistema', 'henrique.carvalho087@empresa.com',  'https://avatar.empresa.com/user087.png', '123456', 1, 1),
('Michele Rodrigues Costa',     'Gestor de TI',             'michele.costa088@empresa.com',      'https://avatar.empresa.com/user088.png', '123456', 2, 1),
('Sergio Almeida Ferreira',     'Analista de Suporte de TI','sergio.ferreira089@empresa.com',    'https://avatar.empresa.com/user089.png', '123456', 3, 1),
('Claudia Nascimento Santos',   'Usuário Operacional',      'claudia.santos090@empresa.com',     'https://avatar.empresa.com/user090.png', '123456', 4, 1),
('Danilo Gomes Ribeiro',        'Administrador do Sistema', 'danilo.ribeiro091@empresa.com',     'https://avatar.empresa.com/user091.png', '123456', 1, 1),
('Monique Lima Araujo',         'Gestor de TI',             'monique.araujo092@empresa.com',     'https://avatar.empresa.com/user092.png', '123456', 2, 1),
('Caio Henrique Souza',         'Analista de Suporte de TI','caio.souza093@empresa.com',         'https://avatar.empresa.com/user093.png', '123456', 3, 1),
('Elaine Carvalho Santos',      'Usuário Operacional',      'elaine.santos094@empresa.com',      'https://avatar.empresa.com/user094.png', '123456', 4, 1),
('Hugo Oliveira Ferreira',      'Administrador do Sistema', 'hugo.ferreira095@empresa.com',     'https://avatar.empresa.com/user095.png', '123456', 1, 1),
('Sonia Rodrigues Almeida',     'Gestor de TI',             'sonia.almeida096@empresa.com',      'https://avatar.empresa.com/user096.png', '123456', 2, 1),
('Jorge Henrique Rocha',        'Analista de Suporte de TI','jorge.rocha097@empresa.com',        'https://avatar.empresa.com/user097.png', '123456', 3, 1),
('Raquel Souza Lima',           'Usuário Operacional',      'raquel.lima098@empresa.com',       'https://avatar.empresa.com/user098.png', '123456', 4, 1),
('Vitor Emanuel Costa',         'Administrador do Sistema', 'vitor.costa099@empresa.com',       'https://avatar.empresa.com/user099.png', '123456', 1, 1),
('Cristiane Ferreira Santos',   'Gestor de TI',             'cristiane.santos100@empresa.com',   'https://avatar.empresa.com/user100.png', '123456', 2, 1),
('Samuel Oliveira Ribeiro',     'Analista de Suporte de TI','samuel.ribeiro101@empresa.com',     'https://avatar.empresa.com/user101.png', '123456', 3, 1),
('Daniela Gomes Araujo',        'Usuário Operacional',      'daniela.araujo102@empresa.com',     'https://avatar.empresa.com/user102.png', '123456', 4, 1),
('Pedro Lucas Martins',         'Administrador do Sistema', 'pedro.martins103@empresa.com',      'https://avatar.empresa.com/user103.png', '123456', 1, 1),
('Leticia Carvalho Costa',      'Gestor de TI',             'leticia.costa104@empresa.com',      'https://avatar.empresa.com/user104.png', '123456', 2, 1),
('Renan Almeida Souza',         'Analista de Suporte de TI','renan.souza105@empresa.com',        'https://avatar.empresa.com/user105.png', '123456', 3, 1),
('Silvia Pereira Lima',         'Usuário Operacional',      'silvia.lima106@empresa.com',        'https://avatar.empresa.com/user106.png', '123456', 4, 1),
('Marcelo Henrique Rocha',      'Administrador do Sistema', 'marcelo.rocha107@empresa.com',      'https://avatar.empresa.com/user107.png', '123456', 1, 1),
('Adriana Fernandes Santos',    'Gestor de TI',             'adriana.santos108@empresa.com',     'https://avatar.empresa.com/user108.png', '123456', 2, 1),
('Bruno Cesar Oliveira',        'Analista de Suporte de TI','bruno.oliveira109@empresa.com',     'https://avatar.empresa.com/user109.png', '123456', 3, 1);


-- ==========================================
-- 9 - INSERTS: equipment
-- ==========================================

INSERT INTO equipment
(`type`, brand, model, asset_number, status, location, responsible, acquisition_date, acquisition_value)
VALUES
-- NOTEBOOKS (10)
('NOTEBOOK', 'Dell',    'Latitude 5420',      'INV-00001', 'EM_USO',        'Matriz - Financeiro',          'Ana Paula Souza',          '2023-01-10', 6500.00),
('NOTEBOOK', 'HP',      'EliteBook 840 G7',   'INV-00002', 'EM_USO',        'Matriz - Controladoria',       'Bruno Henrique Lima',       '2022-11-05', 5800.00),
('NOTEBOOK', 'Lenovo',  'ThinkPad T14',       'INV-00003', 'EM_MANUTENCAO', 'Matriz - TI',                  'Gabriela Antunes Costa',    '2021-06-18', 6200.00),
('NOTEBOOK', 'Dell',    'Latitude 5430',      'INV-00004', 'EM_USO',        'Filial SP - Atendimento',      'Diego Augusto Rocha',       '2023-05-20', 6700.00),
('NOTEBOOK', 'Acer',    'Aspire 5',           'INV-00005', 'EM_ESTOQUE',    'Matriz - Almoxarifado TI',     'Henrique Cardoso Alves',    '2024-02-15', 4200.00),
('NOTEBOOK', 'Apple',   'MacBook Air M1',     'INV-00006', 'EM_USO',        'Matriz - Diretoria',           'Marina Duarte Fonseca',     '2022-03-12', 9500.00),
('NOTEBOOK', 'Apple',   'MacBook Pro 14',     'INV-00007', 'EM_USO',        'Matriz - Diretoria',           'Vitor Hugo Carvalho',       '2023-09-01', 14500.00),
('NOTEBOOK', 'Samsung', 'Galaxy Book2',       'INV-00008', 'EM_USO',        'Filial RJ - Comercial',        'Felipe Moreira Santos',     '2023-04-09', 5400.00),
('NOTEBOOK', 'Dell',    'Latitude 7410',      'INV-00009', 'EM_USO',        'Filial SP - Financeiro',       'Eduarda Ferraz Silva',      '2021-12-21', 7200.00),
('NOTEBOOK', 'Lenovo',  'Yoga Slim 7',        'INV-00010', 'DESCARTADO',    'Matriz - Almoxarifado TI',     'Equipe TI',                 '2018-08-30', 4300.00),
('NOTEBOOK', 'Dell',    'Latitude 5440',      'INV-00040', 'EM_USO',        'Matriz - TI',                  'Ana Paula Souza',           '2024-01-10', 7000.00),
('NOTEBOOK', 'Dell',    'Latitude 5440',      'INV-00044', 'EM_MANUTENCAO', 'Matriz - Suporte',             'Ana Paula Souza',           '2024-01-10', 8000.00),



-- DESKTOPS (6)
('DESKTOP',  'Dell',    'OptiPlex 7090',      'INV-00011', 'EM_USO',        'Filial SP - Atendimento',      'Lucas Fernando Ribeiro',    '2020-03-12', 4200.00),
('DESKTOP',  'HP',      'ProDesk 400 G6',     'INV-00012', 'EM_ESTOQUE',    'Matriz - Almoxarifado TI',     'Estoque TI',                '2019-09-20', 3800.00),
('DESKTOP',  'Lenovo',  'ThinkCentre M720',   'INV-00013', 'EM_USO',        'Matriz - Suporte TI',          'Gustavo Henrique Peixoto',  '2021-02-10', 3900.00),
('DESKTOP',  'Dell',    'Precision 3650',     'INV-00014', 'EM_USO',        'Matriz - Engenharia',          'Rafael Teixeira Cunha',     '2022-07-25', 8200.00),
('DESKTOP',  'Positivo','Master D340',        'INV-00015', 'EM_USO',        'Filial RJ - Backoffice',       'Helena Barcellos Dias',     '2020-11-03', 2700.00),
('DESKTOP',  'HP',      'ProDesk 600 G5',     'INV-00016', 'DESCARTADO',    'Matriz - Almoxarifado TI',     'Equipe TI',                 '2017-05-18', 3600.00),

-- MONITORES (4)
('MONITOR',  'LG',      '29UM69G',            'INV-00017', 'EM_USO',        'Matriz - Diretoria',           'Marina Duarte Fonseca',     '2022-02-01', 1600.00),
('MONITOR',  'Samsung', 'U28E590D',           'INV-00018', 'EM_ESTOQUE',    'Matriz - Almoxarifado TI',     'Estoque TI',                '2021-12-15', 1400.00),
('MONITOR',  'Dell',    'UltraSharp U2419H',  'INV-00019', 'EM_USO',        'Matriz - Financeiro',          'Patricia Gomes Freitas',    '2023-06-10', 1800.00),
('MONITOR',  'AOC',     'Hero 27G2',          'INV-00020', 'EM_USO',        'Filial SP - TI',               'Thiago Oliveira Rezende',   '2022-09-05', 1550.00),

-- SERVIDORES (4)
('SERVIDOR', 'Dell',    'PowerEdge R740',     'INV-00021', 'EM_USO',        'Matriz - CPD',                 'Equipe Infra',              '2020-08-30', 35000.00),
('SERVIDOR', 'HP',      'ProLiant DL380',     'INV-00022', 'EM_USO',        'Matriz - CPD',                 'Equipe Infra',              '2019-04-10', 32000.00),
('SERVIDOR', 'Lenovo',  'ThinkSystem SR650',  'INV-00023', 'EM_USO',        'Matriz - CPD',                 'Sabrina Moura Neves',       '2021-03-22', 34000.00),
('SERVIDOR', 'Dell',    'PowerEdge R750',     'INV-00024', 'EM_MANUTENCAO', 'Matriz - CPD',                 'Gustavo Henrique Peixoto',  '2023-01-18', 38000.00),

-- IMPRESSORAS (3)
('IMPRESSORA','HP',     'LaserJet M404dn',    'INV-00025', 'EM_USO',        'Matriz - Recepcao',            'Diego Augusto Rocha',       '2021-01-25', 2500.00),
('IMPRESSORA','Brother','HL-L5100DN',         'INV-00026', 'EM_USO',        'Filial SP - Administrativo',   'Fernanda Campos Tavares',   '2020-10-05', 2300.00),
('IMPRESSORA','Epson',  'EcoTank L6191',      'INV-00027', 'EM_ESTOQUE',    'Matriz - Almoxarifado TI',     'Estoque TI',                '2022-04-14', 2100.00),

-- SWITCH (2)
('SWITCH',   'Cisco',   'WS-C3850-48TS',      'INV-00031', 'EM_USO',        'Matriz - TI',                  'Gustavo Henrique Peixoto',  '2023-02-01', 1500.00),
('SWITCH',   'Cisco',   'CBS350-8T-E-2G',     'INV-00032', 'RESERVADO',     'Matriz - Sala de Redes',       'Equipe Infra',              '2025-01-10', 1200.00),

-- SMARTPHONE (2)
('SMARTPHONE','Samsung', 'Galaxy A52',         'INV-00033', 'EM_USO',        'Matriz - TI',                  'Gustavo Henrique Peixoto',  '2023-03-15', 1200.00),
('SMARTPHONE','Samsung', 'Galaxy A523',        'INV-00034', 'EM_USO',        'Matriz - TI',                  'Gustavo Henrique Peixoto',  '2023-03-15', 1500.00),




-- ROTEADORES (3)
('ROTEADOR', 'Cisco',   'RV340',              'INV-00028', 'EM_USO',        'Filial SP - Sala de Redes',    'Equipe Infra',              '2023-05-05', 1800.00),
('ROTEADOR', 'TP-LINK', 'Archer C80',         'INV-00029', 'DESCARTADO',    'Matriz - Almoxarifado TI',     'Equipe Infra',              '2018-07-19',  450.00),
('ROTEADOR', 'MikroTik','hEX S',              'INV-00030', 'EM_USO',        'Filial RJ - Sala de Redes',    'Bruno Henrique Lima',       '2022-08-11', 1200.00);



-- ==========================================
-- 10 - SCRIPT PARA LEITURA DE TABELAS DA DB
-- ==========================================

SELECT 
    c.TABLE_NAME              AS tabela,
    c.COLUMN_NAME             AS coluna,
    c.ORDINAL_POSITION        AS ordem,
    c.COLUMN_TYPE             AS tipo_completo,
    c.DATA_TYPE               AS tipo_base,
    c.CHARACTER_MAXIMUM_LENGTH AS tam_max,
    c.NUMERIC_PRECISION       AS precisao,
    c.NUMERIC_SCALE           AS escala,
    c.IS_NULLABLE             AS aceita_nulo,
    c.COLUMN_KEY              AS tipo_chave,
    CASE 
        WHEN c.COLUMN_KEY = 'PRI' THEN 'PRIMARY KEY'
        WHEN c.COLUMN_KEY = 'UNI' THEN 'UNIQUE'
        WHEN c.COLUMN_KEY = 'MUL' THEN 'INDEX/FK POSSÍVEL'
        ELSE 'SEM ÍNDICE ESPECIAL'
    END                       AS descricao_chave,
    c.COLUMN_DEFAULT          AS valor_default,
    c.EXTRA                   AS extra
FROM INFORMATION_SCHEMA.COLUMNS c
WHERE c.TABLE_SCHEMA = 'itinventory_equip'
ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION;




