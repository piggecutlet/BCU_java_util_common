package common.battle.attack;

import common.battle.entity.Cannon;
import common.util.unit.Trait;

import java.util.List;

/**
 * にゃんこ砲種別を攻撃ビットと対象種別へ変換する通常攻撃。
 * 砲種によって死体・地中対象や右端除外の判定を追加する。
 */
public class AttackCanon extends AttackSimple {

	public AttackCanon(Cannon c, int ATK, List<Trait> tr, int eab, Proc pro, float p0, float p1, int duration) {
		super(null, c, ATK, tr, eab, pro, p0, p1, true, null, 9, false, duration);
		canon = c.id > 2 ? 1 << (c.id - 1) : 1 << c.id;
		excludeRightEdge = c.id == 6;
		waveType |= WT_CANN;
		if (canon == 16)
			touch = TCH_UG | TCH_N | TCH_CORPSE;
		if (canon == 32)
			touch = TCH_N | TCH_CORPSE;
	}

}
