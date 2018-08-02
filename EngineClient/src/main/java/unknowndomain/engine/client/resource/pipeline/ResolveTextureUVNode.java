package unknowndomain.engine.client.resource.pipeline;

import com.google.common.collect.Lists;
import de.matthiasmann.twl.utils.PNGDecoder;
import unknowndomain.engine.api.resource.Resource;
import unknowndomain.engine.api.resource.ResourceManager;
import unknowndomain.engine.api.util.DomainedPath;
import unknowndomain.engine.client.texture.GLTexture;

import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

class ResolveTextureUVNode implements ResourcePipeline.Node {
    private int dimension = 256;

    @Override
    public void process(ResourcePipeline.Context context) throws Exception {
        ResourceManager manager = context.manager();
        List<Model> models = context.in("models");
        Map<String, TexturePart> required = new HashMap<>();
        List<TexturePart> parts = Lists.newArrayList();
        for (Model model : models) {
            for (String variant : model.textures.keySet()) {
                String path = model.textures.get(variant);
                while (path.startsWith("#")) {
                    String next = model.textures.get(path.substring(1, path.length()));
                    if (next == null) {
                        path = null;
                        break;
                    }
                    path = next;
                }
                if (path == null)
                    continue;
                model.textures.put(variant, path);
                if (!required.containsKey(path)) {
                    Resource resource = manager.load(new DomainedPath("", path));
                    PNGDecoder decoder = new PNGDecoder(resource.open());
                    ByteBuffer buf = ByteBuffer.allocateDirect(4 * decoder.getWidth() * decoder.getHeight());
                    decoder.decodeFlipped(buf, decoder.getWidth() * 4, PNGDecoder.Format.RGBA);
                    TexturePart part = new TexturePart(decoder.getWidth(), decoder.getHeight(), buf);
                    required.put(path, part);
                    parts.add(part);
                }
            }
        }

        int dimension = stitch(parts);
        GLTexture glTexture = new GLTexture(glGenTextures());
        glBindTexture(GL_TEXTURE_2D, glTexture.id);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        nglTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, dimension, dimension, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, 0);
        for (TexturePart part : parts) {
            glTexSubImage2D(GL_TEXTURE_2D, 0, part.offsetX, part.offsetY, part.width, part.height,
                    GL_RGBA, GL_UNSIGNED_BYTE, part.buffer);
        }
        glGenerateMipmap(GL_TEXTURE_2D);


        for (Model m : models) {
            for (Model.Element e : m.elements) {
                Lists.newArrayList(e.faces.up, e.faces.down, e.faces.north, e.faces.west, e.faces.east, e.faces.south)
                        .forEach((face) -> {
                            String path = m.textures.get(face.texture);
                            TexturePart p = required.get(path);
                            face.uv[0] = (face.uv[0] + p.offsetX) / dimension;
                            face.uv[1] = (face.uv[1] + p.offsetY) / dimension;
                            face.uv[2] = (face.uv[2] + p.offsetX) / dimension;
                            face.uv[3] = (face.uv[3] + p.offsetY) / dimension;
                        });
            }
        }
        context.out("MappedResolvedModels", models);
        context.out("TextureMap", glTexture);
    }

    int stitch(List<TexturePart> parts) {
        parts.sort(Comparator.<TexturePart>comparingInt(a -> a.height).reversed());
        PriorityQueue<FreeSpace> queue = new PriorityQueue<>(Comparator.comparingInt(a -> a.height * a.width));
        List<FreeSpace> unaccepted = new ArrayList<>();
        int dimension = this.dimension;
        queue.add(new FreeSpace(0, 0, dimension, dimension));

        for (TexturePart part : parts) {
            boolean accepted = false;
            while (!accepted) {
                while (!queue.isEmpty()) {
                    FreeSpace free = queue.poll();
                    if (free.accept(queue, part)) {
                        accepted = true;
                        break;
                    }
                    unaccepted.add(free);
                }
                unaccepted.addAll(queue);
                queue.clear();

                for (FreeSpace a : unaccepted) {
                    if (!a.valid) continue;
                    for (int i = 0; i < unaccepted.size(); i++) {
                        FreeSpace b = unaccepted.get(i);
                        if (a == b || !b.valid || !a.merge(b)) continue;
                        i = 0;
                    }
                }
                for (FreeSpace space : unaccepted)
                    if (space.valid)
                        queue.add(space);

                unaccepted.clear();
                if (!accepted) {
                    queue.add(new FreeSpace(dimension, 0, dimension, dimension));
                    queue.add(new FreeSpace(0, dimension, dimension + dimension, dimension));
                    dimension = dimension * 2;
                }
            }
        }
        return dimension;

    }

    static class TexturePart {
        int width;
        int height;
        int offsetX;
        int offsetY;
        ByteBuffer buffer;

        TexturePart(int width, int height, ByteBuffer buffer) {
            this.width = width;
            this.height = height;
            this.buffer = buffer;
        }

        @Override
        public String toString() {
            return "TexturePart{" +
                    "width=" + width +
                    ", height=" + height +
                    ", offsetX=" + offsetX +
                    ", offsetY=" + offsetY +
                    '}';
        }
    }

    class FreeSpace {
        int x, y, width, height;
        boolean valid = true;

        FreeSpace(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean merge(FreeSpace other) {
            if (x + width == other.x && other.height == this.height) {
                this.width += other.width;
                other.valid = false;
                return true;
            }
            if (x + height == other.y && other.width == this.width) {
                this.height += other.height;
                other.valid = false;
                return true;
            }
            return false;
        }

        boolean accept(PriorityQueue<FreeSpace> others, TexturePart part) {
            if (part.width <= width && part.height <= height) {
                int remainedWidth = width - part.width;
                int remainedHeight = height - part.height;
                part.offsetX = x;
                part.offsetY = y;
                if (remainedHeight != 0 && remainedWidth != 0) {
                    others.add(new FreeSpace(x + part.width, y, remainedWidth, part.height));
                    others.add(new FreeSpace(x, y + part.height, width + remainedWidth, remainedHeight));
                } else if (remainedWidth != 0)
                    others.add(new FreeSpace(x + part.width, y, remainedWidth, part.height));
                else
                    others.add(new FreeSpace(x, y + part.height, part.width, remainedHeight));
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "FreeSpace{" +
                    "x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}