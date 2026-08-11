package common.battle.attack;

import common.CommonStatic;
import common.battle.entity.AbEntity;
import common.battle.entity.Entity;
import common.util.pack.EffAnim.DefEff;

import java.util.HashSet;
import java.util.Set;

/**
 * 通常波動・小波動を一定間隔で次の区間へ連鎖させる。
 * 攻撃前に波動無効対象を検査し、検出時は同じ連鎖の全区間を停止する。
 */
public class ContWaveDef extends ContWaveAb {

	protected ContWaveDef(AttackWave a, float p, int layer, int delay) {
		super(a, p, (a.dire == 1 ? a.waveType == WT_MINI ? effas().A_E_MINIWAVE : effas().A_E_WAVE : a.waveType == WT_MINI ? effas().A_MINIWAVE : effas().A_WAVE).getEAnim(DefEff.DEF), layer, delay);
		soundEffect = SE_WAVE;

		maxt -= 1;
		anim.setTime(1);
		waves = new HashSet<>();
		waves.add(this);
	}

	protected ContWaveDef(AttackWave a, float p, int layer, int delay, Set<ContWaveAb> waves) {
		super(a, p, (a.dire == 1 ? a.waveType == WT_MINI ? effas().A_E_MINIWAVE : effas().A_E_WAVE : a.waveType == WT_MINI ? effas().A_MINIWAVE : effas().A_WAVE).getEAnim(DefEff.DEF), layer, delay);
		soundEffect = SE_WAVE;

		maxt -= 1;
		anim.setTime(1);
		this.waves = waves;
		this.waves.add(this);
	}

	@Override
	public void update() {
		tempAtk = false;
		boolean isMini = atk.waveType == WT_MINI;
		// 本家との比較から推測した攻撃フレーム
		int attack = (isMini ? 4 : 6);
		if (t == 0)
			CommonStatic.setSE(soundEffect);
		if (t <= attack) {
			atk.capture();
			for (AbEntity e : atk.capt)
				if ((e.getAbi() & AB_WAVES) > 0) {
					if (e instanceof Entity)
						((Entity) e).anim.getEff(STPWAVE);
					if (t < 0)
						CommonStatic.setSE(soundEffect);
					deactivate();
					return;
				}
		}
		if (!activate)
			return;
		if (t == (isMini ? W_MINI_TIME : W_TIME)) {
			if (isMini && atk.proc.MINIWAVE.lv > 0)
				nextWave();
			else if (!isMini && atk.getProc().WAVE.lv > 0)
				nextWave();
		}
		if (t == attack) {
			sb.getAttack(atk);
			tempAtk = true;
		}
		if (maxt == t)
			activate = false;
		if (t >= 0)
			anim.update(false);
		t++;
	}

	@Override
	public void updateAnimation() {
		if (t >= 0)
			anim.update(false);
	}

	@Override
	protected void nextWave() {
		int dire = atk.model.getDire();
		float np = pos + W_PROG * dire;

		if (atk.proc.WAVE.inverted && atk.waveType == WT_WAVE) {
			np = pos - W_PROG * dire;
		}
		if (atk.proc.MINIWAVE.inverted && atk.waveType == WT_MINI) {
			np = pos - W_PROG * dire;
		}

		int wid = dire == 1 ? W_E_WID : W_U_WID;
		new ContWaveDef(new AttackWave(atk.attacker, atk, np, wid), np, layer, 0, waves);
	}

	@Override
	public boolean IMUTime() {
		return (atk.attacker.getAbi() & AB_TIMEI) != 0;
	}
}
