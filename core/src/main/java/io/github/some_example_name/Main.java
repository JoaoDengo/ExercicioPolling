package io.github.some_example_name;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.some_example_name.pool.Balao;
import io.github.some_example_name.pool.Flecha;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private static final float WORLD_WIDTH = 625f;
    private static final float WORLD_HEIGHT = 264f;

    private static final float ARCO_WIDTH = 110f;
    private static final float ARCO_HEIGHT = 180f;
    private static final float ARCO_X = 8f;
    private static final float ARCO_Y = 70f;

    private static final float FLECHA_WIDTH = 64f;
    private static final float FLECHA_HEIGHT = 16f;
    private static final float BALAO_WIDTH = 48f;
    private static final float BALAO_HEIGHT = 64f;

    private static final float VELOCIDADE_FLECHA = 420f;
    private static final float INTERVALO_DISPARO = 0.20f;

    private static final float VELOCIDADE_BALAO = 72f;
    private static final float INTERVALO_NOVA_FILA = 2.2f;
    private static final float BALAO_INICIO_X = 220f;
    private static final float ESPACAMENTO_BALAO = 26f;
    private static final int BALOES_POR_FILA = 15;
    private static final float BALAO_Y_INICIAL = -BALAO_HEIGHT - 6f;

    private SpriteBatch batch;
    private Texture terrenoTexture;
    private Texture arcoTexture;
    private Texture flechaTexture;
    private Texture balaoTexture;

    private final Array<Flecha> flechasAtivas = new Array<>();
    private final Array<Balao> baloesAtivos = new Array<>();

    private final Pool<Flecha> flechaPool = new Pool<Flecha>(8, 40) {
        @Override
        protected Flecha newObject() {
            return new Flecha();
        }
    };

    private final Pool<Balao> balaoPool = new Pool<Balao>(8, 30) {
        @Override
        protected Balao newObject() {
            return new Balao();
        }
    };

    private float tempoAteNovaFila;
    private float recargaDisparo;

    @Override
    public void create() {
        batch = new SpriteBatch();
        terrenoTexture = new Texture("terreno.png");
        arcoTexture = new Texture("arco.png");
        flechaTexture = new Texture("flecha.png");
        balaoTexture = new Texture("balao.png");

        tempoAteNovaFila = 0f;
        recargaDisparo = 0f;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        atualizar(delta);

        ScreenUtils.clear(0.05f, 0.6f, 0.08f, 1f);
        batch.begin();
        batch.draw(terrenoTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        for (Balao balao : baloesAtivos) {
            batch.draw(balaoTexture, balao.position.x, balao.position.y, BALAO_WIDTH, BALAO_HEIGHT);
        }

        batch.draw(arcoTexture, ARCO_X + ARCO_WIDTH, ARCO_Y, -ARCO_WIDTH, ARCO_HEIGHT);

        for (Flecha flecha : flechasAtivas) {
            batch.draw(flechaTexture, flecha.position.x, flecha.position.y, FLECHA_WIDTH, FLECHA_HEIGHT);
        }

        batch.end();
    }

    private void atualizar(float delta) {
        recargaDisparo = Math.max(0f, recargaDisparo - delta);

        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            tentarDisparar();
        }

        tempoAteNovaFila -= delta;
        while (tempoAteNovaFila <= 0f) {
            spawnFilaBaloes();
            tempoAteNovaFila += INTERVALO_NOVA_FILA;
        }

        atualizarFlechas(delta);
        atualizarBaloes(delta);
        verificarColisoes();
    }

    private void tentarDisparar() {
        if (recargaDisparo > 0f) {
            return;
        }

        Flecha flecha = flechaPool.obtain();
        float spawnX = ARCO_X + ARCO_WIDTH - 6f;
        float spawnY = ARCO_Y + ARCO_HEIGHT * 0.63f - FLECHA_HEIGHT * 0.5f;
        flecha.init(spawnX, spawnY, VELOCIDADE_FLECHA, 0f);

        flechasAtivas.add(flecha);
        recargaDisparo = INTERVALO_DISPARO;
    }

    private void spawnFilaBaloes() {
        for (int i = 0; i < BALOES_POR_FILA; i++) {
            float spawnX = BALAO_INICIO_X + i * ESPACAMENTO_BALAO;
            if (spawnX + BALAO_WIDTH > WORLD_WIDTH - 4f) {
                break;
            }

            Balao balao = balaoPool.obtain();
            balao.init(spawnX, BALAO_Y_INICIAL, 0f, VELOCIDADE_BALAO);
            baloesAtivos.add(balao);
        }
    }

    private void atualizarFlechas(float delta) {
        for (int i = flechasAtivas.size - 1; i >= 0; i--) {
            Flecha flecha = flechasAtivas.get(i);
            flecha.update(delta, WORLD_WIDTH, WORLD_HEIGHT);
            if (!flecha.alive) {
                flechasAtivas.removeIndex(i);
                flechaPool.free(flecha);
            }
        }
    }

    private void atualizarBaloes(float delta) {
        for (int i = baloesAtivos.size - 1; i >= 0; i--) {
            Balao balao = baloesAtivos.get(i);
            balao.update(delta, WORLD_WIDTH, WORLD_HEIGHT);
            if (!balao.alive) {
                baloesAtivos.removeIndex(i);
                balaoPool.free(balao);
            }
        }
    }

    private void verificarColisoes() {
        for (int i = flechasAtivas.size - 1; i >= 0; i--) {
            Flecha flecha = flechasAtivas.get(i);

            for (int j = baloesAtivos.size - 1; j >= 0; j--) {
                Balao balao = baloesAtivos.get(j);
                if (!sobrepoe(flecha, balao)) {
                    continue;
                }

                baloesAtivos.removeIndex(j);
                balaoPool.free(balao);
            }
        }
    }

    private boolean sobrepoe(Flecha flecha, Balao balao) {
        return flecha.position.x < balao.position.x + BALAO_WIDTH
            && flecha.position.x + FLECHA_WIDTH > balao.position.x
            && flecha.position.y < balao.position.y + BALAO_HEIGHT
            && flecha.position.y + FLECHA_HEIGHT > balao.position.y;
    }

    @Override
    public void dispose() {
        for (Flecha flecha : flechasAtivas) {
            flechaPool.free(flecha);
        }
        flechasAtivas.clear();

        for (Balao balao : baloesAtivos) {
            balaoPool.free(balao);
        }
        baloesAtivos.clear();

        batch.dispose();
        terrenoTexture.dispose();
        arcoTexture.dispose();
        flechaTexture.dispose();
        balaoTexture.dispose();
    }
}
