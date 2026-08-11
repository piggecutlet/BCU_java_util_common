package common.util;

/**
 * 戦闘中も複製せず参照を共有する値・資源を示すマーカー。
 * {@link BattleObj} の深いコピー処理では、この型を実装するオブジェクトは同一参照のまま保持される。
 */
public interface BattleStatic {

	/**
	 * BattleObj の conflict() と戻り値を衝突させ、BattleObj の派生型による同時実装を防ぐ。
	 */
	default void conflict() {
	}

}
