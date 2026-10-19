# Agenda API

Backend Kotlin/Spring Boot da aplicação de agenda inteligente, incluindo o núcleo de lembretes e a integração VOX.

## API Core

O núcleo da API oferece cadastro de usuários, categorias padrão, criação e manutenção de lembretes, prioridades, recorrência, encerramento, histórico de concluídos, validações básicas e testes iniciais.

## Endpoint

`POST /api/vox/transcriptions`

Corpo:

```json
{ "text": "Me lembre de entregar o trabalho amanhã às 18h" }
```

Fluxo: valida texto → encaminha ao microserviço Python (`POST /interpret`) → grava os campos interpretados na coleção MongoDB `vox_reminders` → devolve `id`, título, data, hora, domínio e prioridade.

Também existe `GET /api/vox/health` para verificar se a API está acessível.

## Configuração

Variáveis de ambiente:

- `MONGODB_URI`: padrão `mongodb://localhost:27017/agenda`.
- `VOX_AI_SERVICE_URL`: padrão `http://localhost:8000`.
- `SERVER_PORT`: padrão `8080`.
- `SERVER_ADDRESS`: padrão `0.0.0.0`.

Com MongoDB e microserviço Python ligados, execute na raiz do projeto:

```bash
./gradlew bootRun
```

No Windows, use `gradlew.bat bootRun`.

## Segurança / implantação

A configuração de CORS e a autorização abertas são destinadas somente ao MVP em desenvolvimento. Antes de publicar, restrinja origens e rotas, integre a autenticação/autorização do restante da agenda e use HTTPS. Os endpoints VOX não recebem áudio, apenas texto.
