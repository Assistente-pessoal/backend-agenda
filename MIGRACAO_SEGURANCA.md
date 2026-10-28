# Plano de migração para autenticação

## Situação anterior

- `users`: e-mails sem normalização padronizada; senha já era salva usando BCrypt no código enviado.
- `reminders`: não possui `ownerId` nos documentos anteriores.
- `vox_reminders`: não possui `ownerId` nos documentos anteriores.
- `reminder_completions`: não possui `ownerId` nos documentos anteriores.
- A API aceitava requisições sem autenticação.

## Comportamento da nova versão

1. Contas novas possuem `emailNormalized` e índice único esparso. O login suporta documentos antigos via busca sem distinção de maiúsculas/minúsculas.
2. Cada novo lembrete recebe `ownerId` do **token validado no servidor**. Para lembretes manuais, leitura, edição e conclusão exigem correspondência entre ID do lembrete e ID do usuário. Para lembretes VOX, o registro também recebe o proprietário, mas a API ainda não tem rota de listagem de VOX.
3. Documentos antigos continuam guardados, com `ownerId` ausente. **Por padrão permanecem inacessíveis via endpoints autenticados**. Nunca é seguro inferir propriedade só pelo conteúdo do texto, pelo horário ou pela pessoa que se cadastrou primeiro.
4. As sessões são guardadas em `auth_sessions`; o índice TTL é apenas limpeza. Mesmo antes de o Mongo remover o registro expirado, a API rejeita o token.

## Passos antes do deploy

1. Backup verificado de `users`, `reminders`, `vox_reminders` e `reminder_completions` (incluindo uma restauração de teste em homologação).
2. Verificar e-mails duplicados apenas por diferenças de caixa/espaços em `users` e resolver conflitos com os envolvidos antes de migrar e-mails antigos para `emailNormalized`.
3. Revisar o frontend para login e envio de tokens. A versão anterior terá `401` em rotas privadas.
4. Fazer rollout inicial em ambiente separado. Testar duas contas, acesso cruzado, expiração, logout, rotas públicas e rotas privadas.
5. Configurar variáveis de ambiente, HTTPS e rate limiting no gateway/WAF. Em produção, manter o MongoDB não exposto à Internet pública.

## Migrar registros antigos com cuidado

A coleção original de lembretes **não fornece uma relação verificável com usuários**. Para migrar, obtenha uma prova independente e confiável de titularidade (ex.: backup/sistema anterior que relacione ID de usuário ao ID do lembrete, ou uma revisão manual confirmada pelo usuário). Atualize **somente** os documentos identificados com segurança. Não execute migração indiscriminada atribuindo todos os registros a uma única conta.

Exemplo ilustrativo para uma associação já validada, **não execute sem conferência dos IDs e backup**:

```javascript
// Em mongosh, após identificar com segurança a titularidade:
db.reminders.updateOne(
  { _id: ObjectId('ID_DO_LEMBRETE_VALIDADO'), ownerId: { $exists: false } },
  { $set: { ownerId: 'ID_DO_USUARIO_CONFIRMADO' } }
);
```

Os IDs de lembretes e usuários podem ser strings ou ObjectId, conforme os documentos reais. Ajuste o filtro depois de conferir o tipo de `_id`, jamais por tentativa.

## Limitações remanescentes

- Sem recuperação de senha / verificação de e-mail / MFA.
- Sem rate limiting distribuído; instale proteção na borda antes de uso aberto.
- Sem refresh token: duração de sessão configurável (1 a 168 horas).
- O histórico unificado VOX + manual ainda não foi implementado.
- Não existe endpoint novo para excluir lembretes nesta etapa.
- Alterações em APIs e sessões precisam ser verificadas com testes end-to-end no ambiente de implantação.
