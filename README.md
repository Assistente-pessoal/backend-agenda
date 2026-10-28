# Synapse — Agenda API

Backend Kotlin / Spring Boot do Synapse, com MongoDB e comunicação com o microserviço Python/FastAPI.

## Autenticação e autorização (etapa 2)

A API usa **tokens opacos Bearer** (256 bits aleatórios), válidos por **24 horas** por padrão. Os tokens são devolvidos ao cliente **somente no login** e são guardados no banco **somente como hash SHA-256** na coleção `auth_sessions`, com índice TTL para limpeza e checagem de expiração em cada requisição. `POST /auth/logout` revoga a sessão imediatamente. Não é JWT, não precisa de chave simétrica nem implementa refresh token nesta etapa.

Senhas são armazenadas com BCrypt, **nunca em texto puro**. E-mails de contas novas são normalizados e possuem índice único no campo `emailNormalized`; contas antigas continuam compatíveis com login por e-mail. O acesso a lembretes é filtrado pelo `ownerId` derivado do token no servidor, e não por parâmetro enviado pelo aplicativo.

### Rotas públicas

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/users` | Criar conta, com validação de nome, e-mail e senha |
| `POST` | `/auth/login` | Validar credenciais e fornecer um token |
| `GET` | `/api/vox/health` | Verificar se a API está disponível |

### Rotas que exigem `Authorization: Bearer <token>`

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/auth/me` | Informações da conta da sessão |
| `POST` | `/auth/logout` | Revogar o token atual |
| `POST` | `/reminders` | Criar um lembrete do usuário |
| `GET` | `/reminders?status=ACTIVE` | Listar apenas lembretes do usuário |
| `PATCH` | `/reminders/{id}` | Editar somente lembrete do usuário |
| `PATCH` | `/reminders/{id}/complete` | Concluir somente lembrete do usuário |
| `POST` | `/api/vox/transcriptions` | Interpretar texto e salvar o lembrete VOX com `ownerId` |

As rotas `GET /reminders` e `POST /api/vox/transcriptions` **ainda não oferecem um histórico unificado**, pois os registros estão nas coleções `reminders` e `vox_reminders`, respectivamente. Isso pertence à etapa 3.

### Exemplos de uso

```bash
# 1. Criar conta (HTTP 201)
curl -i -X POST http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Usuário Teste","email":"teste@example.com","password":"umaSenhaDeTeste123"}'

# 2. Login (HTTP 200)
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"teste@example.com","password":"umaSenhaDeTeste123"}'

# Resposta:
# {"tokenType":"Bearer","accessToken":"syn_...","expiresAt":"...","user":{"id":"...","name":"...","email":"..."}}

# 3. Substitua o token abaixo pelo accessToken recebido:
TOKEN='syn_COLE_AQUI_O_TOKEN_REAL'
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/auth/me

# 4. Criar um lembrete do usuário
curl -i -X POST http://localhost:8080/reminders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"title":"Enviar relatório"}'

# 5. Criar um lembrete interpretado pelo microserviço
curl -i -X POST http://localhost:8080/api/vox/transcriptions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"Me lembre de entregar o relatório amanhã às 10h"}'

# 6. Sair: este token não funcionará mais
curl -i -X POST -H "Authorization: Bearer $TOKEN" http://localhost:8080/auth/logout
```

**Comportamento esperado:** `401` sem token, inválido ou expirado; `404` para recurso que não existe ou pertence a outra pessoa; `409` para e-mail duplicado; `400` para requisições inválidas. Um login bem-sucedido responde com `Cache-Control: no-store`.

### Configuração

| Variável | Padrão | Observação |
|---|---|---|
| `MONGODB_URI` | `mongodb://localhost:27017/agenda` | Use segredo de deploy no servidor, nunca no frontend |
| `VOX_AI_SERVICE_URL` | `http://localhost:8000` | Serviço Python |
| `SERVER_PORT` | `8080` | Porta HTTP |
| `SERVER_ADDRESS` | `0.0.0.0` | Para testes na rede local |
| `AUTH_SESSION_HOURS` | `24` | Intervalo permitido: 1 a 168 horas |
| `AUTH_CORS_ALLOWED_ORIGINS` | vazio | Lista separada por vírgulas, origens explícitas; sem `*` |

**CORS** é relevante para clientes web em navegadores, **não** para requisições nativas do React Native. Para um painel web com origem `https://app.example.com`, use `AUTH_CORS_ALLOWED_ORIGINS=https://app.example.com`. Não configure curingas em produção.

Para desenvolvimento, inicialize MongoDB e o microserviço Python e então execute:

```bash
# Linux/macOS
bash gradlew bootRun
# Windows
.\gradlew.bat bootRun
```

Para testes:

```bash
bash gradlew test
```

### Cuidados importantes de implantação

1. **Faça backup do MongoDB e teste em homologação antes de publicar.** Registros antigos, sem `ownerId`, **não serão apresentados nem alterados por nenhum usuário**, até serem associados ao proprietário mediante identificação confiável; **não** atribua automaticamente a primeira conta.
2. **Atualize o aplicativo para realizar login antes de enviar lembretes** e adicionar o cabeçalho Authorization em chamadas privadas. O aplicativo antigo (sem login) receberá `401` após o deploy; coordene o lançamento.
3. Armazene o token no dispositivo com armazenamento seguro (ex.: `expo-secure-store`), nunca no código, `.env` público ou logs. Reabra a tela de login se houver `401` por expiração ou revogação.
4. Use **HTTPS** em produção, TLS também entre os serviços quando necessário, permissões mínimas no MongoDB e controle de acesso ao ambiente de deploy.
5. **Antes de produção pública:** implemente rate limiting distribuído nas rotas `/auth/login` e `/users`, controles antibot, monitoramento seguro, recuperação de senha e política de verificação de e-mail conforme necessidade. Estes recursos não estão incluídos neste pacote.
6. Tokens não possuem refresh automático. Após expirar, a pessoa precisa entrar novamente; isso evita introduzir outro ciclo de credenciais antes de o frontend estar integrado.

Veja [`INTEGRACAO_FRONTEND.md`](INTEGRACAO_FRONTEND.md) para um cliente React Native e [`MIGRACAO_SEGURANCA.md`](MIGRACAO_SEGURANCA.md) para a estratégia de dados antigos.
