package common.util;

import java.util.Random;

/**
 * {@link BattleObj} とともに複製できる、戦闘計算用の疑似乱数状態。
 * next 系は現在の seed から値を生成して次の seed へ進めるが、{@link #irDouble()} は共有の Math.random() を使用する。
 */
public class CopRand extends BattleObj {

	private long seed;

	public CopRand(long s) {
		seed = s;
	}

	public double irDouble() {
		return Math.random();
	}

	public double nextDouble() {
		Random r = new Random(seed);
		seed = r.nextLong();
		return r.nextFloat();
	}

	public float nextFloat() {
		Random r = new Random(seed);
		seed = r.nextLong();
		return r.nextFloat();
	}

}
