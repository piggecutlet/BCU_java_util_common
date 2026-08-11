package common.battle.attack;

import common.battle.StageBasis;
import common.battle.entity.Entity;
import common.util.BattleObj;

/**
 * 攻撃生成元の位置・方向・能力と、所属ステージを攻撃判定へ提供する基底型。
 * 実体攻撃、にゃんこ砲、スニャイパーで共通利用される。
 */
public abstract class AtkModelAb extends BattleObj {

	public final StageBasis b;

	public AtkModelAb(StageBasis bas) {
		b = bas;
	}

	public abstract int getAbi();

	public abstract int getDire();

	public abstract float getPos();

	public void invokeLater(AttackAb atk, Entity e) {
	}

	protected int getLayer() {
		return 10;
	}

}
