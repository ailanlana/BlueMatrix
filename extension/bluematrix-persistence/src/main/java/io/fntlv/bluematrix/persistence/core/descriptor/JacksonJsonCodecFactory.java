package io.fntlv.bluematrix.persistence.core.descriptor;

import br.com.finalcraft.everydatabase.codec.Codec;
import br.com.finalcraft.everydatabase.codec.JacksonJsonCodec;

public final class JacksonJsonCodecFactory implements BlueEntityCodecFactory {
    @Override
    public <V> Codec<V> create(Class<V> entityType) {
        return new JacksonJsonCodec<V>(entityType);
    }
}
