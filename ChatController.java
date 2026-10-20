package br.com.companheiro;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@RestController
public class ChatController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20)).build();

    public record ChatLine(String role, String content) {}
    public record ChatRequest(String message, String name, List<ChatLine> history) {}
    public record ChatReply(String reply) {}

    @PostMapping("/api/chat")
    public ChatReply chat(@RequestBody ChatRequest request) {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A variável OPENAI_API_KEY não foi configurada no servidor.");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mensagem vazia.");
        }

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5.5"));
            ArrayNode input = body.putArray("input");

            ObjectNode instructions = input.addObject();
            instructions.put("role", "system");
            instructions.put("content", """
                Você é um companheiro virtual acolhedor chamado %s, um gato de pixel art.
                Converse em português brasileiro natural, como uma pessoa gentil e atenta.
                Responda diretamente ao que a pessoa disse; faça no máximo uma pergunta de cada vez.
                Varie o jeito de escrever e evite repetir fórmulas, slogans e frases prontas.
                Não use repetidamente expressões como "confie em mim", "sem julgamentos" ou
                "você não precisa fingir". Demonstre atenção de modo indireto, mencionando
                detalhes concretos que a pessoa compartilhou. Não diga que é humano nem prometa
                segredo absoluto. Respostas curtas a médias, sem exagerar nos emojis.
                Você é apoio emocional, não substitui pessoas de confiança ou profissionais.
                Se a pessoa falar de perigo imediato ou vontade de se machucar, responda com calma,
                incentive-a a procurar agora um adulto de confiança e ajuda de emergência local.
                Não ajude com tarefas escolares; redirecione gentilmente para conversar sobre
                como a pessoa está se sentindo.
                """.formatted(safeName(request.name())));

            if (request.history() != null) {
                for (ChatLine line : request.history().stream().skip(Math.max(0, request.history().size() - 12)).toList()) {
                    if (line == null || line.content() == null) continue;
                    String role = "assistant".equals(line.role()) ? "assistant" : "user";
                    ObjectNode item = input.addObject();
                    item.put("role", role);
                    item.put("content", line.content().substring(0, Math.min(1500, line.content().length())));
                }
            }
            ObjectNode current = input.addObject();
            current.put("role", "user");
            current.put("content", request.message().substring(0, Math.min(1500, request.message().length())));

            HttpRequest apiRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/responses"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> apiResponse = http.send(apiRequest, HttpResponse.BodyHandlers.ofString());
            if (apiResponse.statusCode() < 200 || apiResponse.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "A API de IA respondeu com status " + apiResponse.statusCode());
            }

            JsonNode json = mapper.readTree(apiResponse.body());
            StringBuilder answer = new StringBuilder();
            JsonNode output = json.path("output");
            for (JsonNode item : output) {
                if ("message".equals(item.path("type").asText())) {
                    for (JsonNode content : item.path("content")) {
                        if ("output_text".equals(content.path("type").asText())) {
                            if (!answer.isEmpty()) answer.append("\n");
                            answer.append(content.path("text").asText(""));
                        }
                    }
                }
            }
            if (answer.isEmpty()) {
                return new ChatReply("Me dá só um instante? Não consegui organizar uma resposta agora.");
            }
            return new ChatReply(answer.toString().trim());
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível conversar com a IA neste momento.");
        }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "Companheiro";
        return name.replaceAll("[\\r\\n]", " ").substring(0, Math.min(30, name.length()));
    }
}
