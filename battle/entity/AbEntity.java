package common.battle.entity;

import common.battle.attack.AttackAb;
import common.util.BattleObj;
import common.util.unit.Trait;

import java.util.List;

/**
 * 戦場で攻撃対象となる実体の共通契約。
 * 現在HP・位置・陣営を保持し、更新を移動処理と反応処理の2段階に分けてステージから呼び出される。
 */
public abstract class AbEntity extends BattleObj {

	/**
	 * {@code health} は現在HP、{@code maxH} は最大HP。
	 * 最大HPは回復上限とHP割合効果の基準にも使う。
	 */
	public long health, maxH;
	/**
	 * 陣営と進行方向。-1は味方、1は敵。
	 */
	public int dire;
	/**
	 * 現在位置。
	 */
	public float pos;
	/**
	 * 割り込みを受けずに移動した最後の位置。
	 */
	public float lastPosition;

	protected AbEntity(int h) {
		if (h <= 0)
			h = 1;
		health = maxH = h;
	}

	public void added(int d, float p) {
		pos = p;
		lastPosition = p;
		dire = d;
	}

	public abstract boolean damaged(AttackAb atk);

	public abstract int getAbi();

	public abstract boolean isBase();

	public abstract void postUpdate();

	public abstract boolean traitCompatible(List<Trait> t, Entity attacker, boolean targetOnly);

	public abstract int touchable();

	public abstract void update();

	public abstract void update2();

	public abstract void updateAnimation();
}
