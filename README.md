# Agenda API

Backend da aplicação de agenda inteligente, responsável pelo gerenciamento de usuários, categorias e lembretes.

## 🚀 API Core

A primeira versão do núcleo da API de lembretes foi implementada na branch `api-core`.

O objetivo dessa entrega é disponibilizar a estrutura principal para gerenciamento dos lembretes e servir como base para as próximas funcionalidades do projeto.

## ✅ Funcionalidades implementadas

* [x] Cadastro de usuário
* [x] Categorias padrão
* [x] Criar lembrete
* [x] Listar e buscar lembretes
* [x] Alterar lembrete
* [x] Prioridade
* [x] Recorrência
* [x] Encerrar lembrete
* [x] Histórico de concluídos
* [x] Validações básicas
* [x] Testes iniciais

## 🏗️ Estrutura

```text
src/
├── main/
│   └── kotlin/
│       └── com/
│           └── agenda/
│               ├── category/
│               ├── config/
│               └── reminder/
│
└── test/
    └── kotlin/
        └── com/
            └── agenda/
                └── ReminderServiceTest.kt
```

## 🧪 Testes

Foram adicionados e ajustados testes automatizados para validar o funcionamento do núcleo da API, incluindo testes relacionados ao serviço de lembretes e à inicialização da aplicação.

## 🌿 Branch

A implementação do API Core está sendo desenvolvida na branch:

```text
api-core
```

A branch será utilizada para validação antes da integração com a `main`.

## 📌 Próximos passos

1. Validar os endpoints e regras implementadas.
2. Executar e revisar os testes.
3. Validar a implementação com o restante do time.
4. Abrir Pull Request da `api-core` para `main`.
5. Integrar as próximas funcionalidades previstas no backlog.
6. Integrar posteriormente o núcleo de lembretes com o fluxo de entrada por voz.

## 🔮 Próximas evoluções

A estrutura atual será utilizada como base para as próximas partes do sistema, incluindo a integração com o fluxo:

```text
Usuário
   ↓
Entrada por voz
   ↓
Reconhecimento de fala
   ↓
Interpretação
   ↓
Classificação da ação
   ↓
API de lembretes
   ↓
Banco de dados
```

> **Status:** API Core implementada e em fase de validação pelo time.
