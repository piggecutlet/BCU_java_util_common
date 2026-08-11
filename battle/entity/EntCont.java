package common.battle.entity;

import common.util.BattleObj;

/**
 * 召喚された実体を指定フレームだけ遅延して戦場へ投入する待機要素。
 * カウントが0になると {@link common.battle.StageBasis} が実体一覧へ移す。
 */
public class EntCont extends BattleObj {

	public Entity ent;

	public int t;

	public EntCont(Entity e, int time) {
		ent = e;
		t = time;
	}

	public void update() {
		if (t > 0)
			t--;
	}

}
