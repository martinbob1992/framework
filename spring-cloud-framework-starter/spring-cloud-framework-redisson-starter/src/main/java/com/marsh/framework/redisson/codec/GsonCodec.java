package com.marsh.framework.redisson.codec;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Gson的序列化编码器，由于默认的编码器保存到redis中不易阅读，这个序列化编码器将对象序列化成json格式数据保存便于阅读
 * ps:如果对象是string类型则保存到redis中还是string数据，不是json格式！
 * @author Marsh
 * @date 2021-11-16日 18:09
 */
public class GsonCodec<T> extends BaseCodec {

    private Gson gson = new Gson();
    private Class c;

    public GsonCodec(Class c) {
        new TypeToken<T>() {}.getType();
        this.c = c;
    }

    private final Encoder encoder = new Encoder() {

        public ByteBuf encode(Object in) throws IOException {
            try {
                if (in instanceof String){
                    ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
                    out.writeCharSequence(in.toString(), Charset.forName("UTF-8"));
                    return out;
                }
                return Unpooled.wrappedBuffer(gson.toJson(in).getBytes("UTF-8"));
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    };
    private final Decoder<Object> decoder = new Decoder<Object>() {
        @Override
        public Object decode(ByteBuf buf, State state) throws IOException {
            return gson.fromJson(new JsonReader(new InputStreamReader(new ByteBufInputStream(buf),"UTF-8")), c);
        }
    };

    public Decoder<Object> getValueDecoder() {
        return decoder;
    }

    public Encoder getValueEncoder() {
        return encoder;
    }
}
