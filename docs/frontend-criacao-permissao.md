# Cadastro de usuário por convite

Todo cadastro novo acontece por convite. O frontend não chama `POST /api/users` nem `POST /api/permissions` nesse fluxo.

## Sequência

1. O administrador envia `POST /api/invitations`, com seu token e os campos `email`, `role` e `bethelId`.
2. O backend salva o convite como `PENDING`, incluindo perfil e Bethel, cria a identidade no Keycloak e envia o e-mail com as ações `VERIFY_EMAIL` e `UPDATE_PASSWORD`. Ainda não há usuário local ou permissão.
3. O convidado abre o link e conclui a verificação do e-mail e a definição de senha no Keycloak.
4. O navegador retorna à URL configurada para o convite. O frontend conclui a autenticação com o Keycloak para obter o access token do convidado; se necessário, inicia o login.
5. O frontend apresenta o formulário de admissão e coleta os dados pessoais.
6. O frontend envia `POST /api/admission/complete`, com o token do convidado e o corpo abaixo.
7. O backend valida o e-mail verificado, o convite pendente e a correspondência do `sub` do token com a identidade registrada no convite. Cria o usuário local e a permissão ativa usando o perfil e o Bethel do convite, e marca o convite como `ACCEPTED`, na mesma transação.
8. Após `201 Created`, o frontend consulta `GET /api/users/me` para carregar o usuário e sua permissão e encaminha para a área correspondente ao perfil.

## Criação do convite: `POST /api/invitations`

Esta é a primeira chamada do cadastro. A tela administrativa deve coletar e-mail, perfil e Bethel do convidado.

```http
POST /api/invitations
Authorization: Bearer <access_token_do_administrador>
Content-Type: application/json
```

```json
{
  "email": "usuario@example.com",
  "role": "MEMBRO_ATIVO",
  "bethelId": 1
}
```

| Campo | Tipo | Regra |
| --- | --- | --- |
| `email` | String | Obrigatório, não vazio e em formato de e-mail válido. |
| `role` | String | Obrigatório; enviar o valor exato do enum. |
| `bethelId` | Número inteiro | Informar o ID do Bethel para o qual o usuário será convidado. A implementação atual valida a existência desse ID. |

Não enviar `userId`, senha ou dados pessoais nessa chamada. A identidade do convidado é criada pelo backend no Keycloak; os dados pessoais serão preenchidos pelo próprio convidado na admissão.

### Perfis e autorização

O enum contém `GRANDE_CONSELHO`, `CONSELHO_DE_BETHEL`, `MEMBRO_ATIVO` e `MEMBRO_DE_MAIORIDADE`.

- Administrador `GRANDE_CONSELHO` pode convidar `CONSELHO_DE_BETHEL`, `MEMBRO_ATIVO` e `MEMBRO_DE_MAIORIDADE` para um Bethel.
- Administrador `CONSELHO_DE_BETHEL` pode convidar apenas `MEMBRO_ATIVO` e `MEMBRO_DE_MAIORIDADE`, exclusivamente para o próprio Bethel.
- Outros perfis não podem criar convites.
- O frontend deve limitar as opções conforme o perfil do administrador; o backend também verifica a autorização.

**Limitação atual do backend:** o convite de `GRANDE_CONSELHO` não está consistente. A validação inicial exige um Bethel existente, mas a criação não associa Bethel para esse perfil e a entidade exige esse vínculo. Não oferecer essa opção como funcional no formulário sem corrigir o backend. Não presumir que `bethelId: null` funciona nesse endpoint.

### Resposta de sucesso

Status: **201 Created**.

```json
{
  "id": 42,
  "email": "usuario@example.com",
  "status": "PENDING",
  "createdAt": "2026-11-13T10:30:00",
  "bethelId": 1
}
```

O `id` dessa resposta é o **ID do convite**, não o ID do futuro usuário. O contrato atual não retorna `role` nem o ID da identidade Keycloak.

Após o sucesso, informar ao administrador que o convite foi enviado e que o cadastro aguarda conclusão pelo convidado. Não chamar `/api/admission/complete` com o token do administrador nem criar uma permissão separadamente.

### Erros e comportamento da tela

| Status | Situação e ação do frontend |
| --- | --- |
| `400` | Dados inválidos, como e-mail incorreto ou perfil ausente. Apresentar a mensagem e os erros por campo quando disponíveis. |
| `401` | Administrador sem autenticação válida. Acionar o tratamento de sessão existente. |
| `403` | Administrador sem autorização para o perfil ou Bethel solicitado. Informar a restrição. |
| `404` | Bethel não encontrado. Atualizar a seleção e permitir correção. |
| `409` | Já existe convite para o e-mail ou identidade no Keycloak. Exibir o conflito; não repetir automaticamente. |
| Falha de rede ou serviço | Informar que não foi possível confirmar a operação e permitir verificar a lista de convites antes de tentar novamente. |

O backend rejeita e-mail já presente na tabela de convites, independentemente do estado do convite. Falhas de integração com o Keycloak ou envio de e-mail também podem impedir o sucesso da chamada.

Durante o envio, desabilitar nova submissão. Em caso de resposta perdida, consultar `GET /api/invitations` com o token do administrador e conferir o e-mail antes de criar outro convite. Essa listagem retorna todos os convites para o Grande Conselho e os convites do próprio Bethel para o Conselho de Bethel; a existência do registro não confirma entrega do e-mail.

## Conclusão da admissão: `POST /api/admission/complete`

Esta chamada acontece depois da configuração da conta no Keycloak, pelo próprio convidado autenticado.

```http
POST /api/admission/complete
Authorization: Bearer <access_token_do_convidado>
Content-Type: application/json
```

Corpo:

```json
{
  "name": "Nome do usuário",
  "birthday": "2000-01-15",
  "city": "Fortaleza",
  "address": "Endereço do usuário",
  "volunteer": false,
  "masonicRelationship": false,
  "member": true,
  "sponsorship": false
}
```

Não enviar `userId`, e-mail, perfil ou Bethel na conclusão. O backend identifica o convidado pelo token e obtém o acesso do convite. O ID local nasce quando a admissão é concluída.

## Estados e tratamento de falhas

- Token válido não significa admissão concluída.
- Antes da conclusão, `/api/users/me` pode retornar `404`, pois o usuário local ainda não existe. Esse retorno isolado não comprova convite válido; o backend valida o convite na conclusão.
- O frontend deve permitir que o convidado autenticado alcance o formulário de admissão, sem exigir uma permissão local prévia nessa tela.
- O backend admite requisições com e-mail verificado quando há usuário ativo ou convite pendente. As funcionalidades protegidas por perfil continuam exigindo permissão.
- Não repetir a conclusão automaticamente se a resposta for perdida. Consultar o perfil para verificar se a operação foi concluída.
- Exibir os erros retornados e bloquear submissões duplicadas durante o envio.

## Critérios de aceite

- [ ] Formulário administrativo coleta e-mail, perfil permitido e Bethel autorizado.
- [ ] Frontend envia `POST /api/invitations` com o token do administrador e trata o `201` como convite pendente, sem exigir ID de usuário.
- [ ] Tela trata validações, conflitos de e-mail, restrições de acesso e falhas de integração.
- [ ] Convites duplicados são evitados durante o envio e na retomada após resposta perdida.
- [ ] Convidado configura a conta no Keycloak e retorna ao frontend autenticado.
- [ ] Convidado sem cadastro local consegue preencher a admissão.
- [ ] Conclusão usa o token do convidado e somente os dados pessoais do contrato.
- [ ] Frontend não cria usuário ou permissão em chamadas adicionais.
- [ ] Após concluir, frontend carrega `/api/users/me` e aplica o acesso retornado.
- [ ] Falhas e respostas perdidas não provocam submissões duplicadas automáticas.
