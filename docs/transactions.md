# Transações financeiras dos Bethéis

Registro administrativo de receitas e despesas. Não executa pagamentos, cobranças
ou escrituração contábil. Os dados ficam disponíveis para uso futuro nos relatórios
RF014 e RF022; consolidação estadual e validação de saldos pertencem ao módulo de relatórios.

## Acesso

Todas as operações exigem JWT, usuário admitido e permissão ativa. O Grande Conselho
pode gerenciar qualquer Bethel; o Conselho de Bethel pode gerenciar somente seu Bethel.
Perfis de membro não acessam as transações financeiras. A autorização é conferida antes
de consultar a existência do Bethel ou da transação.

## API

Base: `/api/bethels/{bethelId}/transactions`.

| Método | Caminho | Resultado |
| --- | --- | --- |
| POST | Base | 201, transação criada |
| GET | Base | 200, página de transações |
| GET | `/{id}` | 200, transação |
| PUT | `/{id}` | 200, transação atualizada |
| DELETE | `/{id}` | 204 |

POST e PUT recebem:

```json
{
  "type": "RECEITA",
  "amount": 125.50,
  "transactionDate": "2026-11-01",
  "description": "Contribuições do mês"
}
```

Todos os campos são obrigatórios. Tipos: `RECEITA` e `DESPESA`. Valor positivo,
até 17 algarismos inteiros e duas casas decimais, sem arredondamento silencioso.
Descrição com até 2000 caracteres e ao menos um caractere não branco.
A data é uma data civil ISO, sem horário. O Bethel vem da rota e o autor do JWT;
ambos são preservados na edição. `createdAt` e `updatedAt` são gerados pelo servidor.
O diagrama não define o significado de `status`; nenhum estado de pagamento ou
aprovação foi introduzido neste escopo.

Filtros opcionais combinados por AND: `startDate`, `endDate` (limites inclusivos),
`type`. Aceita somente um dos limites de data. Intervalo invertido é inválido.
Paginação: `page=0`, `size=20` (máximo 100), por data e ID decrescentes.
Retorno: `content`, `number`, `size`, `totalElements`, `totalPages`.

Exemplo: `?startDate=2026-11-01&endDate=2026-11-30&type=DESPESA&page=0&size=20`.

Mantém a composição de filtros opcionais da BE-13 (consultas existentes de usuários
e Bethéis), acrescentando os campos próprios de transações. Nenhum filtro existente
é alterado. Não havia filtro financeiro previamente implementado na base disponível.

Erros: 400 para campos/filtros inválidos; 401 sem autenticação; 403 sem permissão;
404 para Bethel ou transação inexistente no escopo autorizado. Um ID de transação
de outro Bethel também retorna 404. Exclusão é física.

## Persistência e testes

Migração Flyway V12 cria `financial_transactions`, chaves estrangeiras, restrições
de valor/tipo/descrição e índices por Bethel, período e tipo.

`TransactionControllerTest` usa segurança, serviço e validação reais com repositórios
simulados. `TransactionRepositoryTest` usa PostgreSQL via Testcontainers para verificar
migração, precisão monetária, filtros inclusivos, isolamento, paginação, edição e exclusão.

```sh
./mvnw -Dtest=TransactionControllerTest,TransactionRepositoryTest test
./mvnw verify
```

Os testes de persistência e a suíte completa exigem Docker.
