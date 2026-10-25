package edu.udb.ucacfc.shared.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.udb.ucacfc.shared.util.Textos;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Recorta los espacios de TODOS los textos del body JSON antes de validar ("06142512-3 " pasa a "06142512-3").
 * Las contrasenas NO se recortan: un espacio dentro de una contrasena es parte de ella.
 * Spring Boot registra automaticamente cualquier bean Module en su ObjectMapper.
 */
@Component
public class TrimStringsModule extends SimpleModule {

    public TrimStringsModule() {
        addDeserializer(String.class, new TrimDeserializer());
    }

    static class TrimDeserializer extends StdScalarDeserializer<String> {
        TrimDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() != JsonToken.VALUE_STRING) {
                return StringDeserializer.instance.deserialize(p, ctxt);
            }
            String valor = p.getText();
            String campo = p.currentName();
            if (campo != null && campo.toLowerCase().contains("password")) {
                return valor;
            }
            return Textos.limpiar(valor);
        }
    }
}
