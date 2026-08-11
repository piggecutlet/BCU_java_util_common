package common.battle.data;

import common.util.BattleStatic;
import common.util.Data;
import common.util.Data.Proc;

/**
 * 1回分の攻撃値、射程、対象、発動能力を攻撃モデルへ渡すための参照契約。
 * 短点・長点は攻撃方向を掛ける前の相対座標として扱われる。
 */
public interface MaskAtk extends BattleStatic {

	default int getAltAbi() {
		return 0;
	}

	int getAtk();

	default int getDire() {
		return 1;
	}

	int getLongPoint();

	default int getMove() {
		return 0;
	}

	boolean getSPtrait();

	Proc getProc();

	int getShortPoint();

	default int getTarget() {
		return Data.TCH_N;
	}

	boolean isOmni();

	boolean isRange();

	default int loopCount() {
		return -1;
	}

}
