# Companheiro Pixel

Projeto inicial com:
- `src/main/resources/static/index.html`: interface pixel art, animação de corações, piscada, rabo balançando e cooldown de 3 segundos.
- Backend Java/Spring Boot: endpoint `/api/chat` conectado à OpenAI Responses API.
- A chave da API fica no servidor, nunca no HTML.

## Importante
O GitHub Pages hospeda apenas o frontend estático. Para a IA funcionar, o backend Java precisa ser executado/implantado em um servidor e o frontend deve apontar para o endereço desse servidor. Este projeto usa `fetch('/api/chat')`, então funciona quando o frontend e backend estão servidos pela mesma aplicação/origem.

## Executar localmente
Requisitos: Java 17+ e Maven.

1. Configure `OPENAI_API_KEY` como variável de ambiente no servidor. Nunca coloque a chave no `index.html` nem publique-a no GitHub.
2. Opcionalmente configure `OPENAI_MODEL` para um modelo disponível na sua conta.
3. No terminal, dentro da pasta do projeto, execute `mvn spring-boot:run`.
4. Abra `http://localhost:8080`.

A API pode ter custos e limites de uso. Confira as condições e preços antes de habilitar para outras pessoas. Não coloque informações pessoais sensíveis no chat.
