package common.util.pack.bgeffect;

import common.CommonStatic;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.util.Data;
import common.util.pack.Background;

/**
 * 共有の星効果を後景、雪効果を前景として組み合わせる複合背景効果。
 * 状態は共有アセット内の各効果インスタンスに保持される。
 */
public class SnowStarBGEffect extends BackgroundEffect {
    @Override
    public void check() {
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_STAR).check();
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_SNOW).check();
    }

    @Override
    public void preDraw(FakeGraphics g, P rect, float siz, float midH) {
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_STAR).preDraw(g, rect, siz, midH);
    }

    @Override
    public void postDraw(FakeGraphics g, P rect, float siz, float midH) {
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_SNOW).postDraw(g, rect, siz, midH);
    }

    @Override
    public void update(int w, float h, float midH) {
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_STAR).update(w, h, midH);
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_SNOW).update(w, h, midH);
    }

    @Override
    public void initialize(int w, float h, float midH, Background bg) {
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_STAR).initialize(w, h, midH, bg);
        CommonStatic.getBCAssets().bgEffects.get(Data.BG_EFFECT_SNOW).initialize(w, h, midH, bg);
    }
}
