package common.battle.entity;

import common.system.P;
import common.system.fake.FakeGraphics;
import common.util.BattleObj;
import common.util.anim.EAnimD;

/**
 * ステージ上の位置・描画層に結び付いた一時エフェクトを進行・描画する。
 * 完了判定は保持するアニメーションへ委譲され、ステージ側が完了済み要素を除去する。
 */
public class EAnimCont extends BattleObj {

	public final float pos;
	public final int layer;
	private final EAnimD<?> anim;
	public final float offsetY;

	public EAnimCont(float p, int lay, EAnimD<?> ead) {
		pos = p;
		layer = lay;
		anim = ead;
		offsetY = 0f;
	}

	public EAnimCont(float p, int lay, EAnimD<?> ead, float offsetY) {
		pos = p;
		layer = lay;
		anim = ead;
		this.offsetY = offsetY;
	}

	public boolean done() {
		return anim.done();
	}

	public void draw(FakeGraphics gra, P p, float psiz) {
		p.y += offsetY * psiz;
		anim.draw(gra, p, psiz);
	}

	public void update() {
		anim.update(false);
	}
}
