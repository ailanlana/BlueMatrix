package io.fntlv.bluematrix.persistence.core.descriptor;

import br.com.finalcraft.everydatabase.codec.Codec;

public interface BlueEntityCodecFactory {
    <V> Codec<V> create(Class<V> entityType);
}
