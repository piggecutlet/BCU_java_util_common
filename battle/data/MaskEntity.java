package common.battle.data;

import common.pack.Identifier;
import common.util.Animable;
import common.util.BattleStatic;
import common.util.Data;
import common.util.Data.Proc;
import common.util.anim.AnimU;
import common.util.anim.AnimU.UType;
import common.util.pack.Soul;
import common.util.unit.AbEnemy;
import common.util.unit.Trait;
import common.util.unit.Unit;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 味方・敵の静的戦闘データを、実体生成と攻撃モデルから共通に参照するための契約。
 * 通常攻撃配列の添字と特殊攻撃の取得規約は実装側で一致させる必要がある。
 */
public interface MaskEntity extends BattleStatic {

	int allAtk();

	int getAbi();

	Proc getAllProc();

	default int getAnimLen() {
		return getPack().anim.getAtkLen();
	}

	int getAtkCount();

	int getAtkLoop();

	MaskAtk getAtkModel(int ind);

	MaskAtk[] getAtks();

	Identifier<Soul> getDeathAnim();

	List<Trait> getTraits(); // TODO: 廃止して Trait.getAllTraits に置き換える

    List<Trait> getTraitsRaw();

	int getHb();

	int getHp();

	int getItv();

	Animable<AnimU<?>, UType> getPack();

	int getPost();

	Proc getProc();

	int getRange();

	MaskAtk getRepAtk();

	default AtkDataModel getResurrection() {
		return null;
	}

	default AtkDataModel getRevenge() {
		return null;
	}

	default AtkDataModel getCounter() {
		return null;
	}

	default AtkDataModel getGouge() {
		return null;
	}

	default AtkDataModel getResurface() {
		return null;
	}

	default AtkDataModel getRevive() {
		return null;
	}

	default AtkDataModel getGlass() {
		return null;
	}

	int getSpeed();

	int getWill();

	int getTBA();
	int getRealTBA();

	default int getTouch() {
		return Data.TCH_N;
	}

	int getWidth();

	boolean isLD();

	default boolean isLD(int ind) {
		return isLD();
	}

	boolean isOmni();

	default boolean isOmni(int ind) {
		return isOmni();
	}

	boolean isRange();

	int[][] rawAtkData();

	int touchBase();

	default Set<AbEnemy> getEnemySummon() {
		return new TreeSet<>();
	}

	default Set<Unit> getUnitSummon() {
		return new TreeSet<>();
	}

}
