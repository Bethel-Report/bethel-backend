# Bethel Report Backend

Backend da aplicação Bethel Report, construído com Spring Boot 4.1.1 e Java 21.

## Pré-requisitos

- **JDK 21+** (compilação e execução)
- **Maven 3.9+** (via Maven Wrapper incluso — não requer instalação)
- **Docker** (para testes com Testcontainers)
- **Node.js 18+** e **Yarn** (apenas para hooks de commit — não é necessário em runtime)

## Configuração do ambiente

```bash
# Clonar o repositório
git clone <url-do-repositorio>
cd bethel-backend

# Instalar hooks de commit (Husky + Commitlint)
yarn install
```

O comando `yarn install` executa automaticamente o script `prepare`, que configura o Husky para interceptar commits e validar as mensagens.

## Ferramentas de qualidade de código

| Ferramenta | Responsabilidade | Configuração |
|---|---|---|
| **Spotless** + Eclipse JDT | Formatação automática (estilo Google) | `config/spotless/eclipse-formatter.xml` |
| **Checkstyle** | Regras de estilo (Google Checks) | `config/checkstyle/suppressions.xml` |
| **PMD** | Análise estática (más práticas, bugs) | `config/pmd/ruleset.xml` |
| **Commitlint** | Validação de mensagens de commit | `.commitlintrc.yml` |
| **Husky** | Hooks Git (commit-msg) | `.husky/commit-msg` |

## Comandos disponíveis

### Formatação

```bash
# Verificar formatação (falha se houver violações)
./mvnw spotless:check

# Corrigir formatação automaticamente
./mvnw spotless:apply
```

### Análise estática

```bash
# Verificar estilo com Checkstyle
./mvnw checkstyle:check

# Verificar com PMD
./mvnw pmd:check
```

### Executar todas as verificações

```bash
# Compila, testa, e executa Spotless + Checkstyle + PMD
./mvnw verify
```

### Testes

```bash
# Executar testes (requer Docker para Testcontainers)
./mvnw test
```

## Conventional Commits

As mensagens de commit seguem o padrão [Conventional Commits](https://www.conventionalcommits.org/), validado automaticamente pelo Husky + Commitlint a cada `git commit`.

### Formato

```
tipo(escopo opcional): descrição
```

### Tipos permitidos

| Tipo | Descrição |
|---|---|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `docs` | Documentação |
| `style` | Estilo (sem mudança de comportamento) |
| `refactor` | Refatoração |
| `perf` | Melhoria de desempenho |
| `test` | Testes |
| `build` | Sistema de build ou dependências |
| `ci` | Integração contínua |
| `chore` | Tarefas de manutenção |
| `revert` | Reversão de alterações |

### Exemplos

Mensagens **válidas**:

```
feat: add user authentication
feat(auth): implement JWT authentication
fix: handle null pointer exception
refactor(user): extract user service
test: add unit tests for user service
docs: update API documentation
```

Mensagens **inválidas**:

```
added authentication        # falta tipo
Fixed a bug                 # falta tipo e formato
update code                 # falta tipo
feat add authentication     # falta dois-pontos após o tipo
```

## Resolução de erros comuns

### Spotless: código fora do padrão

```bash
# Corrigir automaticamente
./mvnw spotless:apply
```

### Checkstyle: violação de estilo

Verifique a mensagem de erro no console — ela indica o arquivo, a linha e a regra violada. Consulte a [documentação do Google Java Style](https://google.github.io/styleguide/javaguide.html) para entender a regra.

Para suprimir uma verificação específica em um arquivo, adicione uma entrada em `config/checkstyle/suppressions.xml`:

```xml
<suppress checks="NomeDoCheck" files="Caminho/do/Arquivo\.java"/>
```

### PMD: violação de análise estática

A mensagem indica a regra, o arquivo e a linha. Para excluir uma regra globalmente, adicione-a ao `config/pmd/ruleset.xml`:

```xml
<exclude name="NomeDaRegra"/>
```

### Commitlint: mensagem de commit rejeitada

Corrija a mensagem seguindo o formato `tipo(escopo): descrição`. Use `git commit --amend` para editar a última mensagem.

## Personalização de regras

### Spotless

Edite `config/spotless/eclipse-formatter.xml` para ajustar a formatação (indentação, largura de linha, posição de chaves, etc).

### Checkstyle

- Regras base: `google_checks.xml` (embutido no Checkstyle)
- Supressões: `config/checkstyle/suppressions.xml`
- Para usar um arquivo de regras completamente customizado, altere `<configLocation>` no `pom.xml`

### PMD

Edite `config/pmd/ruleset.xml` para incluir ou excluir regras. Consulte a [documentação do PMD](https://docs.pmd-code.org/latest/) para a lista completa de regras.

### Commitlint

Edite `.commitlintrc.yml` para adicionar ou modificar tipos permitidos e regras de validação.

## CI/CD

O projeto inclui um workflow do GitHub Actions (`.github/workflows/ci.yml`) que executa automaticamente em pushes e pull requests para `main`:

1. Configura JDK 21 e Node.js 22
2. Instala dependências do Commitlint
3. Valida a mensagem do último commit
4. Executa `./mvnw verify` (compilação + testes + Spotless + Checkstyle + PMD)


## BE-08 — Responsáveis administrativos dos Bethéis

O responsável administrativo é um usuário com permissão ativa de perfil
`CONSELHO_DE_BETHEL`, vinculado a um Bethel. Apenas o `GRANDE_CONSELHO`
pode atribuir, alterar, remover, ativar ou desativar esse perfil e consultar
os responsáveis. O Conselho de Bethel continua podendo gerenciar os perfis
de membros do próprio Bethel, sem promover usuários a responsáveis.

| Operação | Método e rota | Corpo |
| --- | --- | --- |
| Associar usuário sem perfil | `POST /api/permissions` | `{"userId":1,"role":"CONSELHO_DE_BETHEL","bethelId":10}` |
| Alterar vínculo ou promover usuário já cadastrado com perfil | `PUT /api/permissions/{permissionId}` | `{"role":"CONSELHO_DE_BETHEL","bethelId":20}` |
| Remover vínculo e perfil | `DELETE /api/permissions/{permissionId}` | — |
| Consultar responsáveis ativos | `GET /api/bethels/{bethelId}/administrators` | — |

A consulta retorna uma lista de `PermissionResponse`, ordenada por ID do vínculo,
com `id`, `userId`, `role`, `bethelId` e `active`; não inclui permissões ou usuários
inativos. Um Bethel existente sem responsáveis retorna `[]`.

O modelo mantém um único perfil por usuário: para um usuário que já tenha perfil,
use a alteração, em vez de criar outro vínculo. A restrição `UNIQUE(user_id)`
existente no banco também impede duplicidade em requisições concorrentes.
Usuários e Bethéis inexistentes retornam `404`; alvos inativos retornam `400`;
vínculos duplicados ou conflitantes retornam `409`. Acesso não autenticado retorna
`401`, e acesso sem permissão retorna `403`. A criação, alteração e reativação
administrativa exigem usuário e Bethel ativos. Não há histórico de gestão nesta
entrega (RF027/RF028 ficam para atividade posterior).


## BE-09 — Publicações e arquivos no MinIO

A API oferece atividades (`ACTIVITY`), orientações (`GUIDANCE`) e comunicados
(`ANNOUNCEMENT`). Somente Grande Conselho e Conselho de Bethel podem criar, editar
ou excluir publicações e anexos. O Conselho de Bethel fica restrito ao próprio
Bethel ativo. A autoria vem do usuário autenticado; editar não altera autor nem
instante de criação. Todas as operações exigem usuário e permissão ativos. Quando a permissão está
associada a um Bethel, esse Bethel também deve estar ativo, inclusive para leitura
e acesso aos anexos.

### Cadastro e edição

`POST /api/publications` retorna `201`; `PUT /api/publications/{id}` retorna `200`
e substitui todos os campos editáveis. Exemplo de atividade:

```json
{
  "title": "Visita solidária",
  "content": "Descrição da atividade realizada.",
  "type": "ACTIVITY",
  "activityDate": "2026-11-01",
  "bethelId": 10,
  "audience": "BETHELS",
  "targetBethelIds": [10]
}
```

Atividades exigem data de realização não futura e Bethel; seu público é o próprio
Bethel. Título e conteúdo são obrigatórios e limitados a 200 e 50.000 caracteres.
Orientações e comunicados não recebem `activityDate`.

O Grande Conselho pode criar publicações estaduais com `bethelId: null` e escolher
`audience: ALL_BETHELS` (sem listas de destinatários), `BETHELS` com
`targetBethelIds`, ou `USERS` com `targetUserIds`. As listas são exclusivas e
limitadas a 500 IDs. Os IDs dos destinatários são usados apenas na distribuição e
não são expostos nas respostas de cadastro, edição, consulta ou feed. Os Bethéis devem existir e estar ativos; usuários selecionados
precisam de usuário e permissão ativos. Publicações locais usam `bethelId` e atingem
somente esse Bethel ou usuários pertencentes a ele. A leitura de publicações locais
confere o vínculo atual: quem muda de Bethel perde esse acesso, mesmo que seu ID
ainda conste na seleção original de destinatários. O Conselho de Bethel não pode
mudar uma publicação para outro Bethel ou abrangência estadual.

Comunicados exigem `displayFrom` e `displayUntil`, em formato ISO de data/hora
(ex.: `2026-11-02T08:00:00`), no horário local do servidor. O fim deve ser posterior
ao início. O intervalo inclui o início e exclui o fim. Fora dele, o conteúdo e
os anexos não ficam disponíveis no feed ou por consulta direta.

### Feed e gerenciamento

- `GET /api/publications?page=0&size=20&order=DESC`: feed do usuário, filtrado pelo
  público-alvo e período de exibição no banco. `order` aceita `ASC` ou `DESC`.
- `type=ANNOUNCEMENT` seleciona os comunicados para o painel de avisos, com o
  mesmo filtro de público e período. Também aceita `ACTIVITY` e `GUIDANCE`.
- `bethelId` opcional filtra o contexto de um Bethel, preservando a autorização.
- `GET /api/publications/{id}`: consulta autorizada de uma publicação visível.
- `management=true` na listagem ou consulta permite aos Conselhos gerenciar as
  publicações de seu contexto, inclusive agendadas, expiradas ou destinadas a
  outros usuários. Esse modo não está disponível para membros.
- `DELETE /api/publications/{id}`: retorna `204` e remove a publicação e os vínculos
  dos anexos; a remoção física no MinIO é processada em fila persistente.

A ordem usa a data da realização para atividades e a data de criação para outras
publicações, com desempate por ID na mesma direção. A página contém `content`,
`number`, `size`, `totalElements` e `totalPages`; tamanhos entre 1 e 100.

### Anexos

Após criar a publicação:

| Operação | Método e rota |
| --- | --- |
| Enviar um arquivo | `POST /api/publications/{id}/attachments` (`multipart/form-data`, campo `file`) |
| Listar metadados | `GET /api/publications/{id}/attachments` |
| Baixar arquivo | `GET /api/publications/{id}/attachments/{attachmentId}` |
| Excluir arquivo | `DELETE /api/publications/{id}/attachments/{attachmentId}` |

Listagem e download também aceitam `management=true`, com as mesmas restrições
administrativas. O acesso aos arquivos sempre passa pela autorização do backend;
nenhuma URL pública ou chave interna é retornada. O download usa
`Content-Disposition: attachment`, `nosniff` e `Cache-Control: no-store`.

O backend valida extensão, assinatura básica do conteúdo e tamanho; o MIME é
atribuído pelo servidor. O nome original não é usado como chave no armazenamento.
Os objetos usam prefixos `fotos/publicacoes/{id}/`, `videos/publicacoes/{id}/` e
`documentos/publicacoes/{id}/`, com identificador aleatório para cada arquivo.
Não há mudança nos diretórios de relatórios, que estão fora desta entrega.

### Configuração local

Preencha as variáveis do `.env-example` no `.env` local, sem versionar segredos:

- `MINIO_ENDPOINT`: `http://localhost:9000` para executar a JVM no host. O serviço
  backend do Compose usa automaticamente `http://minio:9000`.
- `MINIO_ACCESS_KEY` e `MINIO_SECRET_KEY`: credenciais locais próprias. No Compose
  elas também configuram a conta administrativa do MinIO de desenvolvimento.
- `MINIO_BUCKET`: padrão `bethel-report`, criado privado no primeiro upload.
- `PUBLICATION_ALLOWED_EXTENSIONS`: lista separada por vírgulas. Suporte a
  `pdf,jpg,jpeg,png,webp,mp4,webm,docx,xlsx,pptx`; habilitar somente os formatos aprovados.
- `PUBLICATION_MAX_FILE_SIZE`: limite por arquivo, com unidade (`MB`, por exemplo).
- `PUBLICATION_MAX_REQUEST_SIZE`: limite da requisição, maior que o limite por
  arquivo para incluir os cabeçalhos multipart.

Os uploads ficam desabilitados até configurar explicitamente os formatos e
limites de negócio. A política é compartilhada pelo Grande Conselho e pelos
Conselhos de Bethel. Não é necessário configurar uploads para usar o CRUD textual.

```bash
docker compose up -d --build minio
```

API MinIO: porta 9000; console: porta 9001, ambas vinculadas ao localhost. O volume
`minio_data` preserva os arquivos. A imagem local é compilada a partir da release
oficial `RELEASE.2025-10-15T17-29-55Z`, pelo Dockerfile em `docker/minio`. O primeiro
build baixa o Go e as dependências do MinIO; builds seguintes reutilizam o cache.
A camada de download de módulos Go é separada da compilação para preservar esses
downloads quando uma tentativa de compilação é interrompida.
A edição comunitária foi arquivada pelo fornecedor; a versão usada deve ser
revista antes de definir a implantação de produção:
[repositório oficial do MinIO](https://github.com/minio/minio).

A limpeza física roda a cada minuto e tenta novamente quando o MinIO está
indisponível. Uploads iniciam um registro de limpeza independente da transação:
se o banco falhar ou o processo for interrompido, o objeto é elegível para remoção
após uma hora; o registro é cancelado no commit do cadastro bem-sucedido. Exclusões
registram a limpeza na mesma transação que remove os metadados.

### Validação

`./mvnw verify` executa testes e verificações de qualidade. Os testes de integração
exigem Docker e incluem a migration PostgreSQL, os filtros reais do feed e o ciclo
upload/download/exclusão no MinIO. Os testes MVC usam os serviços reais e substituem
somente persistência e armazenamento, cobrindo CRUD, públicos-alvo, datas,
autorização e validações dos anexos.


Para reutilizar a imagem já compilada pelo Compose nos testes, execute
`./mvnw -Dminio.test.image=bethel-minio:RELEASE.2025-10-15T17-29-55Z verify`.
Sem essa opção, o teste constrói a imagem a partir do mesmo Dockerfile oficial.
