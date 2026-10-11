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
