# Integração do frontend Synapse (Android / Expo)

A API mantém os formatos dos lembretes já conhecidos, mas **passa a exigir autenticação** em rotas privadas.

**Não incluí alterações no frontend** porque o ZIP da versão estável não foi disponibilizado nesta etapa. O trecho abaixo é um exemplo de integração — adapte ao serviço HTTP já existente em vez de duplicá-lo.

Instale o armazenamento seguro usando a versão compatível com o Expo do seu projeto:

```bash
npx expo install expo-secure-store
```

Ao adicionar um módulo nativo que não esteja na build existente, gere uma **nova development build / APK**. Não presuma que o módulo esteja disponível no Expo Go.

```ts
// src/services/authApi.ts (exemplo; ajuste os caminhos à sua estrutura)
import * as SecureStore from 'expo-secure-store';

const API = (process.env.EXPO_PUBLIC_API_URL ?? 'https://vox-backend-latest.onrender.com')
  .replace(/\/$/, '');
const TOKEN_KEY = 'synapse_access_token';

type User = { id: string; name: string; email: string };
type LoginResponse = {
  tokenType: 'Bearer';
  accessToken: string;
  expiresAt: string;
  user: User;
};

export async function signIn(email: string, password: string): Promise<User> {
  const response = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!response.ok) throw new Error('Não foi possível entrar. Verifique as credenciais.');
  const data: LoginResponse = await response.json();
  await SecureStore.setItemAsync(TOKEN_KEY, data.accessToken);
  return data.user;
}

// Use este método apenas nas rotas protegidas.
export async function authenticatedFetch(path: string, options: RequestInit = {}) {
  const token = await SecureStore.getItemAsync(TOKEN_KEY);
  if (!token) throw new Error('Sessão não iniciada.');
  const headers = new Headers(options.headers);
  headers.set('Authorization', `Bearer ${token}`);
  const response = await fetch(`${API}${path}`, { ...options, headers });
  if (response.status === 401) {
    await SecureStore.deleteItemAsync(TOKEN_KEY);
    // O estado global de autenticação deve voltar para a tela de login.
    throw new Error('Sua sessão expirou. Entre novamente.');
  }
  return response;
}

export async function signOut(): Promise<void> {
  const token = await SecureStore.getItemAsync(TOKEN_KEY);
  if (token) {
    try {
      await fetch(`${API}/auth/logout`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      });
    } finally {
      await SecureStore.deleteItemAsync(TOKEN_KEY);
    }
  }
}
```

**Fluxo sugerido:** criar conta (`POST /users`), entrar (`POST /auth/login`), guardar o token criptografado pelo armazenamento seguro do Android, enviar `Authorization: Bearer ...` em `/reminders`, `/api/vox/transcriptions`, consultar `/auth/me` ao abrir o app, e revogar em `/auth/logout`. Se `expiresAt` estiver no passado ou o backend devolver `401`, pedir login novamente.

Não armazene senha. Não envie o `ownerId` no JSON: o servidor decide o proprietário a partir da sessão. Evite imprimir ou registrar o cabeçalho Authorization. Ao sair, limpe também os dados sensíveis em memória (último lembrete, avisos que possam conter conteúdo pessoal).
