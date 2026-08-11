package common.battle.data;

import common.util.Data.Proc;

/**
 * {@link DefaultData} の最大3回攻撃を、添字付きの {@link MaskAtk} として公開するアダプター。
 * 射程配列が短い旧データでは0番の長射程設定へフォールバックする。
 */
public class DataAtk implements MaskAtk {

	public final int index;

	public final DefaultData data;

	public DataAtk(DefaultData data, int index) {
		this.index = index;
		this.data = data;
	}

	@Override
	public int getAtk() {
		switch (index) {
		case 0:
			return data.atk;
		case 1:
			return data.atk1;
		case 2:
			return data.atk2;
		default:
			return 0;
		}
	}

	@Override
	public boolean isOmni() {
		return data.ldr[index] < 0;
	}

	@Override
	public int getLongPoint() {
		if (index >= data.lds.length)
			return data.lds[0] + data.ldr[0];
		return data.lds[index] + data.ldr[index];
	}

	@Override
	public Proc getProc() {
		return data.proc;
	}

	@Override
	public boolean getSPtrait() { return false; }

	@Override
	public int getShortPoint() {
		if (index >= data.lds.length)
			return data.lds[0];
		return data.lds[index];
	}

	@Override
	public boolean isRange() {
		return data.isrange;
	}
}
