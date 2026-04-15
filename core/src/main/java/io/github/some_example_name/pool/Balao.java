package io.github.some_example_name.pool;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool;

public class Balao implements Pool.Poolable {
    public final Vector2 position;
    public final Vector2 velocity;
    public boolean alive;

    public Balao() {
        position = new Vector2();
        velocity = new Vector2();
        alive = false;
    }

    public void init(float posX, float posY, float velX, float velY) {
        position.set(posX, posY);
        velocity.set(velX, velY);
        alive = true;
    }

    @Override
    public void reset() {
        position.setZero();
        velocity.setZero();
        alive = false;
    }

    public void update(float delta, float worldWidth, float worldHeight) {
        if (!alive) {
            return;
        }

        position.mulAdd(velocity, delta);
        if (isOutOfScreen(worldWidth, worldHeight)) {
            alive = false;
        }
    }

    private boolean isOutOfScreen(float worldWidth, float worldHeight) {
        float margin = 80f;
        return position.x < -margin || position.x > worldWidth + margin
            || position.y < -margin || position.y > worldHeight + margin;
    }
}
