package common.battle.entity;

import common.CommonStatic;
import common.CommonStatic.BattleConst;
import common.battle.StageBasis;
import common.battle.attack.*;
import common.battle.data.*;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.P;
import common.system.fake.FakeGraphics;
import common.system.fake.FakeTransform;
import common.util.BattleObj;
import common.util.Data;
import common.util.Data.Proc.POISON;
import common.util.Data.Proc.REVIVE;
import common.util.anim.AnimU.UType;
import common.util.anim.EAnimD;
import common.util.anim.EAnimI;
import common.util.anim.EAnimU;
import common.util.pack.EffAnim;
import common.util.pack.EffAnim.*;
import common.util.pack.Soul;
import common.util.stage.StageLimit;
import common.util.unit.Level;
import common.util.unit.Trait;

import java.util.*;

/**
 * 味方・敵に共通する戦闘中の状態機械。
 * 移動・攻撃・ノックバック・潜行・復活・死亡をフレーム単位で進行し、被ダメージは
 * {@link #postUpdate()} まで蓄積してからHPと割り込みへ確定する。
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
public abstract class Entity extends AbEntity {
	public enum KillMode {
		NORMAL,
		SELF_DESTRUCT,
		SPIRIT
	}

	public static class AnimManager extends BattleObj {

		private final Entity e;
		private final int[][] status;

		/**
		 * 死亡演出の残り時間。-1は生存中、正数は死亡演出中、0は演出完了。
		 */
		public int dead = -1;

		/**
		 * ノックバック演出。{@code null} はノックバック外。
		 */
		private EAnimD<KBEff> back;

		private final EAnimU anim;

		public EAnimD<ZombieEff> corpse;

		/**
		 * 昇天演出。{@code null} は未生成。
		 */
		private EAnimI soul;

		public EAnimD<DefEff> smoke;

		public int smokeLayer;

		public int smokeX;

		/**
		 * 一時応答エフェクトの残り時間。
		 */
		private int efft;

		/**
		 * 一時応答エフェクトの種別。
		 */
		private byte eftp;

		/**
		 * 実体上に表示する能力エフェクト。添字は {@code Data.A_*} 定数に対応する。
		 */
		private final EAnimD<?>[] effs = new EAnimD[A_TOT];

		private AnimManager(Entity ent, EAnimU ea) {
			e = ent;
			anim = ea;
			status = e.status;
		}

		/**
		 * 死亡・復活・ノックバック状態に応じた実体アニメーションを描画する。
		 */
		public void draw(FakeGraphics gra, P p, float siz) {
			if (dead > 0 && soul != null) {
				// 本家との比較から推測した高さ補正
				p.y -= 100 * siz;
				soul.draw(gra, p, siz);
				return;
			}

			FakeTransform at = gra.getTransform();

			if (corpse != null) {
				corpse.paraTo(back);
				P corpseP = P.newP(p.x - 25f, p.y);
				corpse.draw(gra, corpseP, siz);
				P.delete(corpseP);
			}

			if (corpse == null || status[P_REVIVE][1] < REVIVE_SHOW_TIME) {
				if (corpse != null) {
					gra.setTransform(at);
					anim.changeAnim(UType.IDLE, false);
				}
			} else {
				gra.delete(at);
				return;
			}

			if(e.data instanceof CustomEntity) {
				if(e.kb.kbType == INT_HB && ((CustomEntity) e.data).kbBounce)
					anim.paraTo(back);
				else if(e.kb.kbType == INT_SW && ((CustomEntity) e.data).bossBounce)
					anim.paraTo(back);
				else if(e.kb.kbType != INT_HB && e.kb.kbType != INT_SW)
					anim.paraTo(back);
			} else {
				anim.paraTo(back);
			}

			if (dead == -1 && (e.kbTime <= 0 && e.kbTime != -1 || e.kb.kbType != INT_WARP))
				anim.draw(gra, p, siz);

			anim.paraTo(null);

			gra.setTransform(at);

			if (CommonStatic.getConfig().ref)
				e.drawAxis(gra, p, siz);

			gra.delete(at);
		}

		/**
		 * 有効中の能力エフェクトを描画する。
		 */
		public void drawEff(FakeGraphics g, P p, float siz) {
			if (dead != -1)
				return;
			if (status[P_WARP][2] != 0)
				return;

			FakeTransform at = g.getTransform();
			int EWID = 36;
			float x = p.x;
			if (effs[eftp] != null) {
				effs[eftp].draw(g, p, siz * 0.75f);
			}

			for(int i = 0; i < effs.length; i++) {
				if(i == A_B || i == A_E_B || i == A_DEMON_SHIELD || i == A_E_DEMON_SHIELD ||
						i == A_COUNTER || i == A_E_COUNTER || i == A_DMGCUT || i == A_E_DMGCUT ||
						i == A_DMGCAP || i == A_E_DMGCAP)
					continue;

				if (((i == A_SLOW || i == A_E_SLOW) && status[P_STOP][0] != 0) || ((i == A_UP || i == A_E_UP) && status[P_WEAK][0] != 0) || ((i == A_CURSE || i == A_E_CURSE) && status[P_SEAL][0] != 0))
					continue;

				EAnimD<?> eae = effs[i];

				if (eae == null)
					continue;

				float offset = 0f;

				g.setTransform(at);
				eae.draw(g, new P(x, p.y+offset), siz * 0.75f);
				x -= EWID * e.dire * siz;
			}

			x = p.x;

			for(int i = 0; i < effs.length; i++) {
				if(i == A_B || i == A_E_B || i == A_DEMON_SHIELD || i == A_E_DEMON_SHIELD ||
						i == A_COUNTER || i == A_E_COUNTER || i == A_DMGCUT || i == A_E_DMGCUT ||
						i == A_DMGCAP || i == A_E_DMGCAP) {
					EAnimD<?> eae = effs[i];

					if(eae == null)
						continue;

					float offset = -25f * siz;

					g.setTransform(at);

					eae.draw(g, new P(x, p.y + offset), siz * 0.75f);
				}
			}

			g.delete(at);
		}

		/**
		 * 指定された能力エフェクトを生成または更新する。
		 */
		@SuppressWarnings("unchecked")
		public void getEff(int t) {
			int dire = e.dire;
			if (t == INV) {
				if (eftp != 0)
					effs[eftp] = null;
				eftp = A_EFF_INV;
				effs[eftp] = effas().A_EFF_INV.getEAnim(DefEff.DEF);
				efft = effas().A_EFF_INV.len(DefEff.DEF);
			} else if (t == P_WAVE) {
				int id = dire == -1 ? A_WAVE_INVALID : A_E_WAVE_INVALID;
				EffAnim<DefEff> eff = dire == -1 ? effas().A_WAVE_INVALID : effas().A_E_WAVE_INVALID;
				effs[id] = eff.getEAnim(DefEff.DEF);
				status[P_WAVE][0] = eff.len(DefEff.DEF);
			} else if (t == STPWAVE) {
				effs[eftp] = null;
				eftp = dire == -1 ? A_WAVE_STOP : A_E_WAVE_STOP;
				EffAnim<DefEff> eff = dire == -1 ? effas().A_WAVE_STOP : effas().A_E_WAVE_STOP;
				effs[eftp] = eff.getEAnim(DefEff.DEF);
				efft = eff.len(DefEff.DEF);
			} else if (t == INVWARP) {
				effs[eftp] = null;
				eftp = dire == -1 ? A_FARATTACK : A_E_FARATTACK;
				EffAnim<DefEff> eff = dire == -1 ? effas().A_FARATTACK : effas().A_E_FARATTACK;
				effs[eftp] = eff.getEAnim(DefEff.DEF);
				efft = eff.len(DefEff.DEF);
			} else if (t == P_STOP) {
				int id = dire == -1 ? A_STOP : A_E_STOP;
				effs[id] = (dire == -1 ? effas().A_STOP : effas().A_E_STOP).getEAnim(DefEff.DEF);
			} else if (t == P_IMUATK) {
				effs[A_IMUATK] = effas().A_IMUATK.getEAnim(DefEff.DEF);
			} else if (t == P_SLOW) {
				int id = dire == -1 ? A_SLOW : A_E_SLOW;
				effs[id] = (dire == -1 ? effas().A_SLOW : effas().A_E_SLOW).getEAnim(DefEff.DEF);
			} else if (t == P_WEAK) {
				if (status[P_WEAK][1] <= 100) {
					int id = dire == -1 ? A_DOWN : A_E_DOWN;
					effs[id] = (dire == -1 ? effas().A_DOWN : effas().A_E_DOWN).getEAnim(DefEff.DEF);
				} else {
					int id = dire == -1 ? A_WEAK_UP : A_E_WEAK_UP;
					effs[id] = (dire == -1 ? effas().A_WEAK_UP : effas().A_E_WEAK_UP).getEAnim(WeakUpEff.UP);
				}
			} else if (t == P_CURSE) {
				int id = dire == -1 ? A_CURSE : A_E_CURSE;
				effs[id] = (dire == -1 ? effas().A_CURSE : effas().A_E_CURSE).getEAnim(DefEff.DEF);
			} else if (t == P_POISON) {
				int mask = status[P_POISON][0];
				EffAnim<?>[] arr = { effas().A_POI0, e.dire == -1 ? effas().A_POI1 : effas().A_POI1_E, effas().A_POI2, effas().A_POI3, effas().A_POI4,
						effas().A_POI5, effas().A_POI6, effas().A_POI7 };
				for (int i = 0; i < A_POIS.length; i++)
					if ((mask & (1 << i)) > 0) {
						int id = A_POIS[i];
						effs[id] = ((EffAnim<DefEff>) arr[i]).getEAnim(DefEff.DEF);
					}

			} else if (t == P_SEAL) {
				effs[dire == -1 ? A_SEAL : A_E_SEAL] = (dire == -1 ? effas().A_SEAL : effas().A_E_SEAL).getEAnim(DefEff.DEF);
			} else if (t == P_STRONG) {
				int id = dire == -1 ? A_UP : A_E_UP;
				effs[id] = (dire == -1 ? effas().A_UP : effas().A_E_UP).getEAnim(DefEff.DEF);
			} else if (t == P_LETHAL) {
				int id = dire == -1 ? A_SHIELD : A_E_SHIELD;
				EffAnim<DefEff> ea = dire == -1 ? effas().A_SHIELD : effas().A_E_SHIELD;
				status[P_LETHAL][1] = ea.len(DefEff.DEF);
				effs[id] = ea.getEAnim(DefEff.DEF);
				CommonStatic.setSE(SE_LETHAL);
			} else if (t == P_WARP) {
				EffAnim<WarpEff> ea = effas().A_W;
				int ind = status[P_WARP][2];
				WarpEff pa = ind == 0 ? WarpEff.ENTER : WarpEff.EXIT;
				e.basis.lea.add(new WaprCont(e.pos, pa, e.currentLayer, anim, e.dire, (e.getAbi() & AB_TIMEI) != 0));
				e.basis.leaSort = true;
				CommonStatic.setSE(ind == 0 ? SE_WARP_ENTER : SE_WARP_EXIT);
				status[P_WARP][ind] = ea.len(pa);

			} else if (t == BREAK_ABI) {
				int id = dire == -1 ? A_B : A_E_B;
				effs[id] = (dire == -1 ? effas().A_B : effas().A_E_B).getEAnim(BarrierEff.BREAK);
				status[P_BREAK][0] = effs[id].len();
				CommonStatic.setSE(SE_BARRIER_ABI);
			} else if (t == BREAK_ATK) {
				int id = dire == -1 ? A_B : A_E_B;
				effs[id] = (dire == -1 ? effas().A_B : effas().A_E_B).getEAnim(BarrierEff.DESTR);
				status[P_BREAK][0] = effs[id].len();
				CommonStatic.setSE(SE_BARRIER_ATK);
			} else if (t == BREAK_NON) {
				int id = dire == -1 ? A_B : A_E_B;
				effs[id] = (dire == -1 ? effas().A_B : effas().A_E_B).getEAnim(BarrierEff.NONE);
				status[P_BREAK][0] = effs[id].len();
				CommonStatic.setSE(SE_BARRIER_NON);
			} else if (t == P_ARMOR) {
				int id = dire == -1 ? A_ARMOR : A_E_ARMOR;
				EffAnim<ArmorEff> eff = dire == -1 ? effas().A_ARMOR : effas().A_E_ARMOR;
				ArmorEff index = status[P_ARMOR][1] >= 0 ? ArmorEff.DEBUFF : ArmorEff.BUFF;
				effs[id] = eff.getEAnim(index);
			} else if (t == P_SPEED) {
				int id = dire == -1 ? A_SPEED : A_E_SPEED;
				EffAnim<SpeedEff> eff = dire == -1 ? effas().A_SPEED : effas().A_E_SPEED;
				SpeedEff index;

				if (status[P_SPEED][2] <= 1) {
					index = status[P_SPEED][1] >= 0 ? SpeedEff.UP : SpeedEff.DOWN;
				} else {
					int speed = e.data.getSpeed();
					index = status[P_SPEED][1] >= (speed > 0 && e.basis.getGlobalSpeed(-1, speed) > -1 ? e.basis.getGlobalSpeed(-1, speed) : speed)
							? SpeedEff.UP : SpeedEff.DOWN;
				}

				effs[id] = eff.getEAnim(index);
			} else if (t == P_LETHARGY) {
				int id = A_LETHARGY;
				EffAnim<LethEff> eff = effas().A_LETHARGY;
				LethEff index;

				if (status[P_LETHARGY][2] <= 1) {
					index = status[P_LETHARGY][1] >= 0 ? LethEff.DEBUFF : LethEff.BUFF;
				} else {
					int modifiedTba = e.applyLethargy(e.waitTime);
					index = status[P_LETHARGY][1] >= (modifiedTba > 0 && e.data.getTBA() > -1 ? e.data.getTBA() : modifiedTba)
							? LethEff.DEBUFF : LethEff.BUFF;
				}

				effs[id] = eff.getEAnim(index);
			} else if (t == P_SPEEDUP) {
				int id = dire == -1 ? A_SPEED : A_E_SPEED;
				EffAnim<SpeedEff> eff = dire == -1 ? effas().A_SPEED : effas().A_E_SPEED;
				SpeedEff index;

				index = status[P_SPEEDUP][0] > 0 ? SpeedEff.UP : SpeedEff.DOWN;

				effs[id] = eff.getEAnim(index);
			} else if (t == HEAL) {
				EffAnim<DefEff> eff = dire == -1 ? effas().A_HEAL : effas().A_E_HEAL;

				effs[dire == -1 ? A_HEAL : A_E_HEAL] = eff.getEAnim(DefEff.DEF);
			} else if (t == SHIELD_HIT) {
				int id = dire == -1 ? A_DEMON_SHIELD : A_E_DEMON_SHIELD;

				EffAnim<ShieldEff> eff = dire == -1 ? effas().A_DEMON_SHIELD : effas().A_E_DEMON_SHIELD;

				boolean half = e.currentShield * 1.0 / (e.getProc().DEMONSHIELD.hp * e.shieldMagnification) < 0.5;

				effs[id] = eff.getEAnim(half ? ShieldEff.HALF : ShieldEff.FULL);
				status[P_DEMONSHIELD][0] = effs[id].len();

				CommonStatic.setSE(SE_SHIELD_HIT);
			} else if (t == SHIELD_BROKEN) {
				int id = dire == -1 ? A_DEMON_SHIELD : A_E_DEMON_SHIELD;

				EffAnim<ShieldEff> eff = dire == -1 ? effas().A_DEMON_SHIELD : effas().A_E_DEMON_SHIELD;

				effs[id] = eff.getEAnim(ShieldEff.BROKEN);
				status[P_DEMONSHIELD][0] = effs[id].len();

				CommonStatic.setSE(SE_SHIELD_BROKEN);
			} else if (t == SHIELD_REGEN) {
				int id = dire == -1 ? A_DEMON_SHIELD : A_E_DEMON_SHIELD;

				EffAnim<ShieldEff> eff = dire == -1 ? effas().A_DEMON_SHIELD : effas().A_E_DEMON_SHIELD;

				effs[id] = eff.getEAnim(ShieldEff.REGENERATION);
				status[P_DEMONSHIELD][0] = effs[id].len();

				CommonStatic.setSE(SE_SHIELD_REGEN);
			} else if (t == SHIELD_BREAKER) {
				int id = dire == -1 ? A_DEMON_SHIELD : A_E_DEMON_SHIELD;

				EffAnim<ShieldEff> eff = dire == -1 ? effas().A_DEMON_SHIELD : effas().A_E_DEMON_SHIELD;

				effs[id] = eff.getEAnim(ShieldEff.BREAKER);
				status[P_DEMONSHIELD][0] = effs[id].len();

				CommonStatic.setSE(SE_SHIELD_BREAKER);
			} else if(t == P_COUNTER) {
				int id = dire == -1 ? A_COUNTER : A_E_COUNTER;

				EffAnim<DefEff> eff = dire == -1 ? effas().A_COUNTER : effas().A_E_COUNTER;

				effs[id] = eff.getEAnim(DefEff.DEF);
			} else if (t == P_DMGCUT) {
				int id = dire == -1 ? A_DMGCUT : A_E_DMGCUT;

				EffAnim<DefEff> eff = dire == -1 ? effas().A_DMGCUT : effas().A_E_DMGCUT;

				effs[id] = eff.getEAnim(DefEff.DEF);
			} else if (t == DMGCAP_FAIL) {
				int id = dire == -1 ? A_DMGCAP : A_E_DMGCAP;

				EffAnim<DmgCap> eff = dire == -1 ? effas().A_DMGCAP : effas().A_E_DMGCAP;

				effs[id] = eff.getEAnim(DmgCap.FAIL);
			} else if (t == DMGCAP_SUCCESS) {
				int id = dire == -1 ? A_DMGCAP : A_E_DMGCAP;

				EffAnim<DmgCap> eff = dire == -1 ? effas().A_DMGCAP : effas().A_E_DMGCAP;

				effs[id] = eff.getEAnim(DmgCap.SUCCESS);
			} else if (t == GUARD_HOLD) {
				int id = A_E_GREEN_GUARD;
				EffAnim<GuardEff> eff = effas().A_E_GUARD;
				effs[id] = eff.getEAnim(GuardEff.NONE);
				CommonStatic.setSE(SE_BARRIER_NON);
			} else if (t == GUARD_BREAK) {
				int id = A_E_GREEN_GUARD;
				EffAnim<GuardEff> eff = effas().A_E_GUARD;
				effs[id] = eff.getEAnim(GuardEff.BREAK);
				CommonStatic.setSE(SE_BARRIER_ABI);
			} else if (t == IMUATK_CD) {
				effs[A_IMUATK] = effas().A_IMUATKCD.getEAnim(DefEff.DEF);
			}
		}

		/**
		 * 状態配列に合わせて能力エフェクトの寿命を更新する。
		 */
		private void checkEff() {
			int dire = e.dire;
			if (efft == 0)
				effs[eftp] = null;
			if (status[P_STOP][0] == 0) {
				byte id = dire == -1 ? A_STOP : A_E_STOP;
				effs[id] = null;
			}
			if (status[P_SLOW][0] == 0) {
				byte id = dire == -1 ? A_SLOW : A_E_SLOW;
				effs[id] = null;
			}
			if (status[P_WEAK][0] == 0) {
				byte id;

				if (status[P_WEAK][1] <= 100) {
					id = dire == -1 ? A_DOWN : A_E_DOWN;
				} else {
					id = dire == -1 ? A_WEAK_UP : A_E_WEAK_UP;
				}

				effs[id] = null;
			}
			if (status[P_CURSE][0] == 0) {
				byte id = dire == -1 ? A_CURSE : A_E_CURSE;
				effs[id] = null;
			}
			if (status[P_IMUATK][0] + status[P_IMUATK][1] + status[P_BSTHUNT][0] + status[P_BSTHUNT][1] == 0) {
				effs[A_IMUATK] = null;
			}
			if (status[P_POISON][0] == 0) {
				for(int i = 0; i < A_POIS.length; i++) {
					effs[A_POIS[i]] = null;
				}
			}
			if (status[P_SEAL][0] == 0) {
				effs[dire == -1 ? A_SEAL : A_E_SEAL] = null;
			}
			if (status[P_LETHAL][1] == 0) {
				byte id = dire == -1 ? A_SHIELD : A_E_SHIELD;
				effs[id] = null;
			} else
				status[P_LETHAL][1]--;
			if (status[P_WAVE][0] == 0) {
				byte id = dire == -1 ? A_WAVE_INVALID : A_E_WAVE_INVALID;
				effs[id] = null;
			} else
				status[P_WAVE][0]--;
			if (Arrays.stream(status[P_STRONG]).allMatch(v -> v == 0)) {
				byte id = dire == -1 ? A_UP : A_E_UP;
				effs[id] = null;
			}
			if (status[P_SPEEDUP][0] == 0 && status[P_SPEED][0] == 0) {
				byte id = dire == -1 ? A_SPEED : A_E_SPEED;
				effs[id] = null;
			}
			if (status[P_BREAK][0] == 0) {
				byte id = dire == -1 ? A_B : A_E_B;
				effs[id] = null;
			} else
				status[P_BREAK][0]--;

			if (status[P_ARMOR][0] == 0) {
				byte id = dire == -1 ? A_ARMOR : A_E_ARMOR;
				effs[id] = null;
			}

			if (status[P_LETHARGY][0] == 0) {
				byte id = A_LETHARGY;
				effs[id] = null;
			}

			byte healId = e.dire == -1 ? A_HEAL : A_E_HEAL;

			if(effs[healId] != null && effs[healId].done()) {
				effs[healId] = null;
			}

			if(effs[A_COUNTER] != null && effs[A_COUNTER].done()) {
				effs[A_COUNTER] = null;
			}

			if(effs[A_E_COUNTER] != null && effs[A_E_COUNTER].done()) {
				effs[A_E_COUNTER] = null;
			}

			if(effs[A_DMGCUT] != null && effs[A_DMGCUT].done()) {
				effs[A_E_DMGCUT] = null;
			}

			if(effs[A_E_DMGCUT] != null && effs[A_E_DMGCUT].done()) {
				effs[A_E_DMGCUT] = null;
			}

			if(effs[A_DMGCAP] != null && effs[A_DMGCAP].done()) {
				effs[A_DMGCAP] = null;
			}

			if(effs[A_E_DMGCAP] != null && effs[A_E_DMGCAP].done()) {
				effs[A_E_DMGCAP] = null;
			}

			efft--;
		}

		/**
		 * 割り込み種別に対応するノックバック演出と時間を設定する。
		 */
		private void kbAnim() {
			int t = e.kb.kbType;
			if (t != INT_SW && t != INT_WARP)
				if(e.status[P_REVIVE][1] >= REVIVE_SHOW_TIME) {
					e.anim.corpse = (e.dire == -1 ? effas().A_U_ZOMBIE : effas().A_ZOMBIE).getEAnim(ZombieEff.BACK);
				} else {
					if (e.anim.corpse != null) {
						if(e.anim.corpse.type == ZombieEff.REVIVE && e.data.getRevive() != null && e.data.getRevive().pre >= e.anim.corpse.len()) {
							e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount() + 4));
						}

						e.anim.corpse = null;

						status[P_REVIVE][1] = 0;
					}

					setAnim(UType.HB, true);
				}
			else
				setAnim(UType.WALK, false);
			if (t == INT_WARP) {
				e.kbTime = status[P_WARP][0];
				getEff(P_WARP);
				status[P_WARP][2] = 1;
			}
			if (t == INT_KB)
				e.kbTime = status[P_KB][0];
			if (t == INT_HB)
				back = effas().A_KB.getEAnim(KBEff.KB);
			if (t == INT_SW)
				back = effas().A_KB.getEAnim(KBEff.SW);
			if (t == INT_ASS)
				back = effas().A_KB.getEAnim(KBEff.ASS);
			if (t != INT_WARP)
				e.kbTime += 1;

			// ゾンビキラー撃破演出
			if (e.health <= 0 && e.zx.tempZK && e.traits.contains(UserProfile.getBCData().traits.get(TRAIT_ZOMBIE))) {
				EAnimD<DefEff> eae = effas().A_Z_STRONG.getEAnim(DefEff.DEF);
				e.basis.lea.add(new EAnimCont(e.pos, e.currentLayer, eae));
				e.basis.leaSort = true;
				CommonStatic.setSE(SE_ZKILL);
			}
		}

		private int deathSurge = 0;

		/**
		 * 撃破時能力を抽選し、死亡演出を設定する。
		 */
		private void kill() {
			if (e.getProc().DEATHSURGE.perform(e.basis.r))
				deathSurge |= 1;
			else if (e.getProc().MINIDEATHSURGE.perform(e.basis.r))
				deathSurge |= 2;

			if (deathSurge != 0) {
				e.weaks.list.clear();
				status[P_WEAK] = new int[PROC_WIDTH];

				soul = UserProfile.getBCData().demonSouls.get(e.dire == -1 ? 1 : 0).getEAnim(UType.SOUL);
				dead = soul.len();
				CommonStatic.setSE(SE_DEATH_SURGE);
			} else {
				boolean isGlass = (e.getAbi() & AB_GLASS) != 0;

				if (isGlass && e.health > 0 && e.data.getGlass() != null)
					e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount() + 5));

				Soul s = isGlass ? null : Identifier.get(e.data.getDeathAnim());
				dead = s != null ? (soul = s.getEAnim(UType.SOUL)).len() : 4;

				if (s != null && s.layertype != CommonStatic.LayerType.ORIG) {
					int slay = e.basis.getValueBetween(s.layer_0, s.layer_1);
					if (s.layertype == CommonStatic.LayerType.SET)
						e.currentLayer = slay;
					else if (s.layertype == CommonStatic.LayerType.RELATIVE)
						e.currentLayer += slay;
				}
			}
		}

		private int setAnim(UType t, boolean skip) {
			if (anim.type != t)
				anim.changeAnim(t, skip);
			return anim.len();
		}

		private void cont() {
			if (anim.type == UType.ATK)
				setAnim(UType.WALK, false);
			if (anim.type == UType.HB) {
				e.interrupt(0, 0f);
				setAnim(UType.WALK, false);
			}
		}

		private void update() {
			checkEff();

			for (int i = 0; i < effs.length; i++)
				if (effs[i] != null)
					effs[i].update(false);

			boolean checkKB = e.kb.kbType != INT_SW && e.kb.kbType != INT_WARP;
			if (status[P_STOP][0] == 0 && (e.kbTime <= 0 && e.kbTime != -1 || checkKB))
				anim.update(false);
			if (back != null)
				back.update(false);
			if (dead > 0) {
				if (soul != null)
					soul.update(false);
				dead--;
			}
			if (anim.done() && anim.type == UType.ENTER)
				setAnim(UType.IDLE, true);
			if (dead >= 0) {
				if (deathSurge > 0 && soul.len() - dead == 21) // 21フレームは本家との比較から推測
					e.aam.getDeathSurge(deathSurge);
				boolean selfDestructed = ((e.getAbi() & AB_GLASS) != 0) && e.health > 0;
				if (!selfDestructed && e.data.getResurrection() != null) {
					AtkDataModel adm = e.data.getResurrection();
					int startTime = soul == null ? 4 : soul.len();
					if ((adm.pre == startTime - dead) || (dead == 0 && adm.pre >= startTime && !e.dead))
						e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount() + 1));
				}
			}
			if(smoke != null) {
				if(smoke.done()) {
					smoke = null;
					smokeLayer = -1;
					smokeX = -1;
				} else {
					smoke.update(false);
				}
			}

			e.dead = dead == 0;
		}

		private void updateAnimation() {
			for (int i = 0; i < effs.length; i++)
				if (effs[i] != null)
					effs[i].update(false);

			boolean checkKB = e.kb.kbType != INT_SW && e.kb.kbType != INT_WARP;

			if (status[P_STOP][0] == 0 && (e.kbTime <= 0 && e.kbTime != -1 || checkKB))
				anim.update(false);
			if (back != null)
				back.update(false);
			if (dead > 0 && soul != null)
				soul.update(false);
			if (corpse != null)
				corpse.update(false);

			if(smoke != null) {
				if(smoke.done()) {
					smoke = null;
					smokeLayer = -1;
					smokeX = -1;
				} else {
					smoke.update(false);
				}
			}
		}
	}

	protected static class AtkManager extends BattleObj {

		/**
		 * 攻撃アニメーションの残り時間。
		 */
		protected int atkTime;

		private int attacksLeft;

		/**
		 * このフレームで発生した攻撃の添字。主に当たり判定表示用。
		 */
		private int tempAtk = -1;

		private final Entity e;

		/**
		 * 通常攻撃スロット数。
		 */
		private final int multi;

		/**
		 * 次に処理する攻撃スロット。
		 */
		private int preID;

		/**
		 * 各攻撃スロットの予備動作時間。
		 */
		private final int[] pres;

		/**
		 * 次の攻撃発生までの残り時間。
		 */
		private int preTime;

		private AtkManager(Entity ent) {
			e = ent;
			int[][] raw = e.data.rawAtkData();
			pres = new int[multi = raw.length];
			for (int i = 0; i < multi; i++)
				pres[i] = raw[i][1];
			attacksLeft = e.data.getAtkLoop();
		}

		protected void startAttack() {
			atkTime = e.data.getAnimLen();
			preID = 0;
			preTime = pres[0];
			e.anim.setAnim(UType.ATK, true);
		}

		private void stopAtk() {
			if (atkTime > 0)
				atkTime = preTime = 0;
		}

		/**
		 * 同時発生スロットを1つ抽選し、全スロット処理後に待機時間へ移る。
		 */
		private void updateAttack() {
			atkTime--;
			if (preTime >= 0) {
				preTime--;
				if (preTime == 0) {
					int atk0 = preID;
					while (true) {
						if (++preID >= multi || pres[preID] != 0)
							break;
					}
					tempAtk = (int) (atk0 + e.basis.r.nextFloat() * (preID - atk0));
					e.basis.getAttack(e.aam.getAttack(tempAtk));
					if (preID < multi) {
						preTime = pres[preID];
					} else {
						attacksLeft--;
						e.waitTime = Math.max(e.applyLethargy(e.data.getTBA()), 0);
					}
				}
			}
			if (atkTime == 0) {
				e.skipSpawnBurrow = false;
				e.anim.setAnim(UType.IDLE, true);
			}
		}
	}

	private static class KBManager extends BattleObj {

		/**
		 * 実行中の割り込み種別。
		 */
		private int kbType;

		private final Entity e;

		/**
		 * ノックバックの残り移動距離。
		 */
		private float kbDis;

		/**
		 * 次のフレームで開始する割り込みの移動距離。
		 */
		private float tempKBdist;

		/**
		 * 次のフレームで開始する割り込み種別。-1は予約なし。
		 */
		private int tempKBtype = -1;

		private float initPos;
		private float kbDuration;
		private float time = 1;

		private KBManager(Entity ent) {
			e = ent;
		}

		/**
		 * 優先度選択済みの割り込み予約を状態へ反映する。
		 */
		private void doInterrupt() {
			int t = tempKBtype;
			if (t == -1)
				return;
			float d = tempKBdist;
			tempKBtype = -1;
			e.walking = false;
			e.atkm.stopAtk();
			e.kbTime = KB_TIME[t];
			kbType = t;
			kbDis = d;
			initPos = e.pos;
			kbDuration = e.kbTime;
			time = 1;
			e.anim.kbAnim();
			e.anim.update();
		}

		private float easeOut(float time, float start, float end, float duration, float dire) {
			time /= duration;
			return -end * time * (time - 2) * dire + start;
		}

		private void interrupt(int t, float d) {
			if (t == INT_ASS && (e.getAbi() & AB_SNIPERI) > 0) {
				e.anim.getEff(INV);
				return;
			}
			if (t == INT_SW && (e.getAbi() & AB_IMUSW) > 0) {
				e.anim.getEff(INV);
				return;
			}
			int prev = tempKBtype;
			if (prev == -1 || KB_PRI[t] >= KB_PRI[prev]) {
				tempKBtype = t;
				tempKBdist = d;
			}
		}

		private void kbmove(float mov) {
			float lim = e.getLim();
			e.pos -= Math.min(mov, lim) * e.dire;
		}

		/**
		 * ノックバック・ワープの移動と演出を進め、終了時にシールド再生・復活・死亡を判定する。
		 */
		private void updateKB() {
			e.kbTime--;
			if (e.kbTime == 0) {
				if(e.isBase) {
					e.anim.setAnim(UType.HB, false);
					return;
				}

				if ((e.getAbi() & AB_GLASS) > 0 && e.atkm.atkTime - 1 == 0 && e.atkm.attacksLeft == 0) {
					e.kill(KillMode.SELF_DESTRUCT);

					return;
				}

				e.anim.back = null;

				if(e.status[P_REVIVE][1] > 0)
					e.anim.corpse = (e.dire == -1 ? effas().A_U_ZOMBIE : effas().A_ZOMBIE).getEAnim(ZombieEff.DOWN);

				e.anim.setAnim(UType.WALK, true);

				kbDuration = 0;
				initPos = 0;
				time = 1;

				if(kbType == INT_HB && e.health > 0 && e.getProc().DEMONSHIELD.hp > 0) {
					e.currentShield = (int) (e.getProc().DEMONSHIELD.hp * e.getProc().DEMONSHIELD.regen * e.shieldMagnification / 100.0);
					if (e.currentShield > e.maxCurrentShield)
						e.maxCurrentShield = e.currentShield;

					e.anim.getEff(SHIELD_REGEN);
				}

				if(kbType == INT_HB && e.data.getRevenge() != null && e.data.getRevenge().pre >= KB_TIME[INT_HB]) {
					e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount()));
				}

				if (e.health <= 0)
					e.preKill();
			} else {
				if (kbType != INT_WARP && kbType != INT_KB) {
					float mov = kbDis / e.kbTime;
					kbDis -= mov;
					kbmove(mov);
				} else if (kbType == INT_KB) {
					if (time == 1) {
						kbDuration = e.kbTime;
					}

					float mov = easeOut(time, initPos, kbDis, kbDuration, -e.dire) - e.pos;
					mov *= -e.dire;

					kbmove(mov);

					time++;
				} else {
					e.anim.setAnim(UType.IDLE, false);
					if (e.status[P_WARP][0] > 0)
						e.status[P_WARP][0]--;
					if (e.status[P_WARP][1] > 0)
						e.status[P_WARP][1]--;
					EffAnim<WarpEff> ea = effas().A_W;
					if (e.kbTime + 1 == ea.len(WarpEff.EXIT)) {
						kbmove(kbDis);
						kbDis = 0;
						e.anim.getEff(P_WARP);
						e.status[P_WARP][2] = 0;
						e.kbTime -= 11;
					}
				}
				if (kbType == INT_HB && e.data.getRevenge() != null) {
					if (KB_TIME[INT_HB] - e.kbTime == e.data.getRevenge().pre)
						e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount()));
				}
			}
		}
	}

	private static class PoisonToken extends BattleObj {

		private final Entity e;

		private final List<POISON> list = new ArrayList<>();

		private PoisonToken(Entity ent) {
			e = ent;
		}

		private void add(POISON ws) {
			if (ws.type.unstackable)
				list.removeIf(e -> e.type.unstackable && type(e) == type(ws));
			ws.prob = 0; // 次の発動までのカウンターとして再利用
			list.add(ws);
			getMax();
		}

		private void damage(int dmg, int type) {
			type &= 3;
			long mul = type == 0 ? 100 : type == 1 ? e.maxH : type == 2 ? e.health : (e.maxH - e.health);
			e.damage += mul * dmg / 100;
		}

		private void getMax() {
			int max = 0;
			for (int i = 0; i < list.size(); i++)
				max |= 1 << type(list.get(i));
			e.status[P_POISON][0] = max;
		}

		private int type(POISON ws) {
			return ws.type.damage_type + (ws.damage < 0 ? 4 : 0);
		}

		private void update() {
			for (int i = 0; i < list.size(); i++) {
				POISON ws = list.get(i);
				if (ws.time > 0) {
					ws.time--;
					ws.prob--;// 発動間隔のカウンターとして再利用
					if (e.health > 0 && ws.prob <= 0) {
						if (!ws.type.ignoreMetal && (e instanceof EEnemy && e.data.getTraits().contains(UserProfile.getBCData().traits.get(TRAIT_METAL)) || (e instanceof EUnit && (e.getAbi() & AB_METALIC) != 0)))
							e.damage += 1;
						else
							damage(ws.damage, type(ws));
						ws.prob += ws.itv;
					}
				}
			}
			list.removeIf(w -> w.time <= 0);
			getMax();
		}

	}

	private static class WeakToken extends BattleObj {

		private final Entity e;

		private final List<int[]> list = new ArrayList<>();

		private WeakToken(Entity ent) {
			e = ent;
		}

		private void add(int[] is) {
			list.add(is);
			getMax();
		}

		private void getMax() {
			int max = 0;
			int val = list.isEmpty() ? 100 : list.get(0)[1];
			for (int i = 0; i < list.size(); i++) {
				int[] ws = list.get(i);
				max = Math.max(max, ws[0]);
				val = Math.min(val, ws[1]);
			}
			e.status[P_WEAK][0] = max;

			float ov = e.status[P_WEAK][1];

			e.status[P_WEAK][1] = val;

			if (ov > 100 && val <= 100) {
				if (e.dire == -1) {
					e.anim.effs[A_WEAK_UP] = null;
				} else {
					e.anim.effs[A_E_WEAK_UP] = null;
				}
			}
		}

		private void update() {
			for (int i = 0; i < list.size(); i++)
				list.get(i)[0]--;
			list.removeIf(w -> w[0] <= 0);
			getMax();
		}

	}

	private static class Barrier extends BattleObj {
		private final Entity e;
		private Barrier (Entity ent) { e = ent; }

		private void update() {
			if (e.status[P_BARRIER][0] > 0) {
				if (e.status[P_BARRIER][2] > 0) {
					e.status[P_BARRIER][2]--;
					if (e.status[P_BARRIER][2] == 0)
						breakBarrier(false);
				}
			} else if (e.status[P_BARRIER][1] > 0) {
				e.status[P_BARRIER][1]--;
				if (e.status[P_BARRIER][1] == 0) {
					e.status[P_BARRIER][0] = e.getProc().BARRIER.type.magnif ? (int) (e.shieldMagnification * e.getProc().BARRIER.health) : e.getProc().BARRIER.health;
					int timeout = e.getProc().BARRIER.timeout;
					if (timeout > 0)
						e.status[P_BARRIER][2] = timeout + effas().A_B.len(BarrierEff.NONE);
					e.anim.getEff(BREAK_NON);
				}
			}
		}

		private void breakBarrier(boolean abi) {
			e.status[P_BARRIER][0] = 0;

			int regen = e.getProc().BARRIER.regentime;
			if (regen > 0) {
				int len = abi ? effas().A_B.len(BarrierEff.BREAK) : effas().A_B.len(BarrierEff.DESTR);
				e.status[P_BARRIER][1] = regen + len;
			}

			e.anim.getEff(abi ? BREAK_ABI : BREAK_ATK);
		}
	}

	private static class ZombX extends BattleObj {

		private final Entity e;

		private final Set<Entity> list = new HashSet<>();

		/**
		 * このフレームの致死攻撃にゾンビキラーが含まれたか。
		 */
		private boolean tempZK;

		private int extraRev = 0;

		private ZombX(Entity ent) {
			e = ent;
		}

		private byte canRevive() {
			if (e.status[P_REVIVE][0] != 0)
				return 1;
			int tot = totExRev();
			if (tot == -1 || tot > extraRev)
				return 2;
			return 0;
		}

		private boolean canZK() {
			if (e.getProc().REVIVE.type.imu_zkill)
				return false;
			for (Entity zx : list)
				if (zx.getProc().REVIVE.type.imu_zkill)
					return false;
			return true;
		}

		private void damaged(AttackAb atk) {
			tempZK |= (atk.abi & AB_ZKILL) > 0 && canZK();
		}

		private void doRevive(int c) {
			int deadAnim = minRevTime();
			EffAnim<ZombieEff> ea = effas().A_ZOMBIE;
			deadAnim += ea.getEAnim(ZombieEff.REVIVE).len();
			e.status[P_REVIVE][1] = deadAnim;
			int maxR = maxRevHealth();
			e.health = e.maxH * maxR / 100;
			if (c == 1)
				e.status[P_REVIVE][0]--;
			else if (c == 2)
				extraRev++;
		}

		private int maxRevHealth() {
			int max = e.getProc().REVIVE.health;
			if (e.status[P_REVIVE][0] == 0)
				max = 0;
			for (Entity zx : list) {
				int val = zx.getProc().REVIVE.health;
				max = Math.max(max, val);
			}
			return max;
		}

		private int minRevTime() {
			int min = e.getProc().REVIVE.time;
			if (e.status[P_REVIVE][0] == 0)
				min = Integer.MAX_VALUE;
			for (Entity zx : list) {
				int val = zx.getProc().REVIVE.time;
				min = Math.min(min, val);
			}
			return min;
		}

		private void postUpdate() {
			if (e.health > 0)
				tempZK = false;
		}

		private boolean prekill() {
			int c = canRevive();
			if (!tempZK && c > 0) {
				int[][] status = e.status;
				doRevive(c);
				// 復活時に継続状態を解除する
				status[P_STOP] = new int[PROC_WIDTH];
				status[P_SLOW] = new int[PROC_WIDTH];
				status[P_WEAK] = new int[PROC_WIDTH];
				status[P_CURSE] = new int[PROC_WIDTH];
				status[P_SEAL] = new int[PROC_WIDTH];
				status[P_STRONG] = new int[PROC_WIDTH];
				status[P_LETHAL] = new int[PROC_WIDTH];
				status[P_POISON] = new int[PROC_WIDTH];
				return true;
			}
			return false;
		}

		private int totExRev() {
			int sum = 0;
			for (Entity zx : list) {
				int val = zx.getProc().REVIVE.count;
				if (val == -1)
					return -1;
				sum += val;
			}
			return sum;
		}

		/**
		 * 周囲の復活付与元を更新し、死体・復活動作を進行する。
		 */
		private void updateRevive() {
			int[][] status = e.status;
			AnimManager anim = e.anim;

			list.removeIf(em -> {
				int conf = em.getProc().REVIVE.type.range_type;
				if (conf == 3)
					return false;
				if (conf == 2 || em.kbTime == -1)
					return em.kbTime == -1;
				return true;
			});
			List<AbEntity> lm = e.basis.inRange(TCH_ZOMBX, e.dire, 0, e.basis.st.len, false);
			for (int i = 0; i < lm.size(); i++) {
				if (lm.get(i) == e)
					continue;
				Entity em = ((Entity) lm.get(i));
				float d0 = em.pos + em.getProc().REVIVE.dis_0;
				float d1 = em.pos + em.getProc().REVIVE.dis_1;
				if ((d0 - e.pos) * (d1 - e.pos) > 0)
					continue;
				if (em.kb.kbType == INT_WARP)
					continue;
				REVIVE.TYPE conf = em.getProc().REVIVE.type;
				if (!conf.revive_non_zombie && e.traits.contains(UserProfile.getBCData().traits.get(TRAIT_ZOMBIE)))
					continue;
				int type = conf.range_type;
				if (type == 0 && (em.touchable() & (TCH_N | TCH_EX)) == 0)
					continue;
				list.add(em);
			}

			if (status[P_REVIVE][1] > 0) {
				EffAnim<ZombieEff> ea = e.dire == -1 ? effas().A_U_ZOMBIE : effas().A_ZOMBIE;
				if (anim.corpse == null) {
					anim.corpse = ea.getEAnim(ZombieEff.DOWN);
					anim.corpse.setTime(0);
				}
				if (status[P_REVIVE][1] == ea.getEAnim(ZombieEff.REVIVE).len() - 2) {
					anim.corpse = ea.getEAnim(ZombieEff.REVIVE);
					anim.corpse.setTime(0);
				}

				boolean isCorpseRevive = anim.corpse != null && anim.corpse.type == ZombieEff.REVIVE && e.data.getRevive() != null;

				if(e.kbTime == 0) {
					status[P_REVIVE][1]--;

					if (isCorpseRevive && anim.corpse.len() - status[P_REVIVE][1] - 2 == e.data.getRevive().pre)
						e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount() + 4));
					if (anim.corpse != null)
						anim.corpse.update(false);
				}

				if (status[P_REVIVE][1] == 0) {
					if (isCorpseRevive && e.data.getRevive().pre >= e.anim.corpse.len())
						e.basis.getAttack(e.aam.getAttack(e.data.getAtkCount() + 4));

					anim.corpse = null;
				}
			}
		}

	}

	private static class SummonManager extends BattleObj {
		public List<Entity> children = new ArrayList<>();

		public void damaged(AttackAb atk, int dmg, boolean proc) {
			for (int i = 0; i < children.size(); i++) {
				if (proc)
					children.get(i).processProcs(atk);
				children.get(i).damage += dmg;
			}
		}
		public void update() {
			children.removeIf(e -> e.anim.dead == 0);
		}
	}

	public final AnimManager anim;

	protected final AtkManager atkm;

	private final ZombX zx = new ZombX(this);

	private final SummonManager bondTree = new SummonManager();

	public final StageBasis basis;

	public final MaskEntity data;

	public int group;

	/**
	 * この実体から派生して存続中の持続攻撃。
	 * 空になるまでは、実体本体の死亡演出完了後も集計対象として戦場に残す。
	 */
	public final List<ContAb> summoned = new ArrayList<>();

	/**
	 * 復活不能まで確定した死亡状態。持続攻撃が残る実体を出撃数から除外するためにも使う。
	 */
	public boolean dead = false;

	/**
	 * 対象ごとに {@code min(確定ダメージ, 攻撃前HP)} を加算した与ダメージ合計。
	 */
	public long damageGiven = 0;

	public long damageTaken = 0;

	public int killCount = 0;

	/**
	 * 現在フレームに命中した攻撃。{@link #postUpdate()} の末尾で消去する。
	 */
	public Set<AttackAb> lastHitBy = new HashSet<>();

	/**
	 * 最終ノックバックへ入ったフレームの攻撃。撃破報酬の攻撃元判定に使う。
	 */
	public Set<AttackAb> lastKilledBy = new HashSet<>();

	public int livingTime = 0;

	private final KBManager kb = new KBManager(this);

	public int currentLayer;

	public int spawnLayer;

	/**
	 * 能力別の実行時状態。第1添字は {@code P_*}、第2添字の意味は能力ごとに異なる。
	 */
	public final int[][] status = new int[PROC_TOT][PROC_WIDTH];

	public List<Trait> traits;

	protected final AtkModelEntity aam;

	/**
	 * 現在フレームの未確定ダメージ。{@link #postUpdate()} でHPへ反映して0へ戻す。
	 */
	private long damage;

	protected boolean isBase;

	/**
	 * ノックバック・潜行状態。
	 * -1は死亡、正数は割り込み残り時間、-2/-3/-4は潜行開始/移動/浮上、0は通常状態。
	 */
	private int kbTime;

	/**
	 * 次の攻撃開始までの待機時間。
	 */
	private int waitTime;

	private float bdist;

	private final PoisonToken pois = new PoisonToken(this);

	/**
	 * ダメージ確定後に発動条件を判定する命中時能力。
	 */
	private final List<AttackAb> tokens = new ArrayList<>();

	/**
	 * 現在フレームの接触有無。
	 */
	private boolean touch;

	/**
	 * 接触対象のうち、対象限定条件を満たす相手がいるか。
	 */
	private boolean touchEnemy;

	private final WeakToken weaks = new WeakToken(this);

	private int altAbi = 0;

	private final Proc sealed = Proc.blank();

	/**
	 * 出現直後の潜行を抑止するフラグ。ボスは最初の移動または攻撃後に潜行可能になる。
	 */
	protected boolean skipSpawnBurrow = false;

	protected boolean moved = false;

	private final Barrier barrier = new Barrier(this);

	public int currentShield, maxCurrentShield;

	/**
	 * 悪魔シールド再生時に適用する生成時HP倍率。
	 */
	private final float shieldMagnification;

	/**
	 * 最終死亡通知を多重実行しないためのフラグ。
	 */
	private boolean killCounted = false;

	private int regentimer;

	public final Proc proc;

	protected Entity(StageBasis b, MaskEnemy de, EAnimU ea, float atkMagnif, float hpMagnif) {
		super((int) (de.getHp() * hpMagnif));
		basis = b;
		data = de;
		proc = data.getProc().clone();
		if (data.getRealTBA() < 0) {
			waitTime = data.getTBA();
		}
		aam = AtkModelEntity.getEnemyAtk(this, atkMagnif);
		anim = new AnimManager(this, ea);
		atkm = new AtkManager(this);
		presetStatus(hpMagnif);
		presetSealedProcs();
		maxCurrentShield = currentShield = (int) (de.getProc().DEMONSHIELD.hp * hpMagnif);
		shieldMagnification = hpMagnif;
		regentimer = getProc().HPREGEN.interval;
	}

	protected Entity(StageBasis b, MaskUnit de, EAnimU ea, float lvMagnif, float tAtk, float tHP, PCoin pc, Level lv) {
		super(
				(pc != null && lv != null && lv.getTalents().length == pc.max.length) ?
				(int) ((1 + (StageLimit.isComboBanned(b.est.lim, Data.C_DEF) ? 0 : b.b.getInc(Data.C_DEF, de.getPack().unit)) * 0.01) * (int) ((int) (Math.round(de.getHp() * lvMagnif) * tHP) * pc.getHPMultiplication(lv.getTalents()))) :
				(int) ((1 + (StageLimit.isComboBanned(b.est.lim, Data.C_DEF) ? 0 : b.b.getInc(Data.C_DEF, de.getPack().unit)) * 0.01) * (int) (Math.round(de.getHp() * lvMagnif) * tHP))
		);
		basis = b;
		data = de;
		proc = data.getProc().clone();
		if (data.getRealTBA() < 0) {
			waitTime = data.getTBA();
		}
		aam = AtkModelEntity.getUnitAtk(this, tAtk, lvMagnif, pc, lv);
		anim = new AnimManager(this, ea);
		atkm = new AtkManager(this);
		status[P_BARRIER][0] = getProc().BARRIER.type.magnif ? (int) (getProc().BARRIER.health * lvMagnif) : getProc().BARRIER.health;
		status[P_BARRIER][1] = getProc().BARRIER.regentime;
		status[P_BARRIER][2] = getProc().BARRIER.timeout;
		status[P_BURROW][0] = getProc().BURROW.count;
		status[P_REVIVE][0] = getProc().REVIVE.count;
		status[P_DMGCUT][0] = getProc().DMGCUT.type.magnif ? (int) (lvMagnif * getProc().DMGCUT.dmg) : getProc().DMGCUT.dmg;
		status[P_DMGCAP][0] = getProc().DMGCAP.type.magnif ? (int) (lvMagnif * getProc().DMGCAP.dmg) : getProc().DMGCAP.dmg;
		presetStatus(lvMagnif);
		presetSealedProcs();
		maxCurrentShield = currentShield = (int) (de.getProc().DEMONSHIELD.hp * lvMagnif);
		shieldMagnification = lvMagnif;
		regentimer = getProc().HPREGEN.interval;
	}

	private void presetStatus(float mag) {
		status[P_BARRIER][0] = getProc().BARRIER.type.magnif ? (int) (getProc().BARRIER.health * mag) : getProc().BARRIER.health;
		status[P_BARRIER][1] = getProc().BARRIER.regentime;
		status[P_BARRIER][2] = getProc().BARRIER.timeout;
		status[P_BURROW][0] = getProc().BURROW.count;
		status[P_REVIVE][0] = getProc().REVIVE.count;
		status[P_DMGCUT][0] = getProc().DMGCUT.type.magnif ? (int) (mag * getProc().DMGCUT.dmg) : getProc().DMGCUT.dmg;
		status[P_DMGCAP][0] = getProc().DMGCAP.type.magnif ? (int) (mag * getProc().DMGCAP.dmg) : getProc().DMGCAP.dmg;
		status[P_HPREGEN][2] = getProc().HPREGEN.scaleWithBuff ? (int) (mag * getProc().HPREGEN.amount) : getProc().HPREGEN.amount;
	}

	private void presetSealedProcs() {
		sealed.BURROW.set(data.getProc().BURROW);
		sealed.REVIVE.count = data.getProc().REVIVE.count;
		sealed.REVIVE.time = data.getProc().REVIVE.time;
		sealed.REVIVE.health = data.getProc().REVIVE.health;
		sealed.SPIRIT.id = data.getProc().SPIRIT.id;
	}

	public void altAbi(int alt) {
		altAbi ^= alt;

	}

	/**
	 * 攻撃を受け、無効・軽減・バリア・シールドを判定して未確定ダメージと能力を蓄積する。
	 */
	@Override
	public boolean damaged(AttackAb atk) {
		damageTaken += atk.atk;

		int dmg = getDamage(atk, atk.atk);
		boolean proc = true;

		if (getProc().HPREGEN.resetWhenDamaged && getProc().HPREGEN.prob > 0) {
			regentimer = getProc().HPREGEN.interval; // 被弾時リセット設定
		}

		if (anim.corpse != null && anim.corpse.type == ZombieEff.REVIVE && status[P_REVIVE][1] >= REVIVE_SHOW_TIME)
			return false;

		Proc.CANNI imuCannon = getProc().IMUCANNON;
		if (atk.canon > 0 && imuCannon.exists() && (atk.canon & imuCannon.type) > 0) {
			if (imuCannon.mult != 100) {
				dmg = dmg * (100 - imuCannon.mult) / 100;
			} else {
				anim.getEff(P_WAVE);
				return false;
			}
		}

		// 波動無効なら以降の被弾処理を行わない
		if (atk.waveType != 5 && ((atk.waveType & WT_WAVE) > 0 || (atk.waveType & WT_MINI) > 0) && atk.canon != 16) {
			if (getProc().IMUWAVE.mult > 0)
				anim.getEff(P_WAVE);
			if (getProc().IMUWAVE.mult == 100)
				return false;
			else
				dmg = dmg * (100 - getProc().IMUWAVE.mult) / 100;
		}

		if ((atk.waveType & WT_MOVE) > 0) {
			if (getProc().IMUMOVING.mult > 0)
				anim.getEff(P_WAVE);
			if (getProc().IMUMOVING.mult == 100)
				return false;
			else
				dmg = dmg * (100 - getProc().IMUMOVING.mult) / 100;
		}

		if ((atk.waveType & (WT_VOLC | WT_MIVC)) > 0) {
			if (getProc().IMUVOLC.mult > 0)
				anim.getEff(P_WAVE);
			if (getProc().IMUVOLC.mult == 100)
				return false;
			else
				dmg = dmg * (100 - getProc().IMUVOLC.mult) / 100;
		}

		if ((atk.waveType & WT_BLST) > 0) {
			if (getProc().IMUBLAST.mult > 0)
				anim.getEff(P_WAVE);
			if (getProc().IMUBLAST.mult == 100)
				return false;
			else
				dmg = dmg * (100 - getProc().IMUBLAST.mult) / 100;
		}

		tokens.add(atk);

		Proc.PTC imuatk = getProc().IMUATK;
		if (imuatk.exists() && (atk.dire == -1 || receive(-1)) || traitCompatible(atk.trait, atk.attacker, false)) {
			if (status[P_IMUATK][0] + status[P_IMUATK][1] == 0 && imuatk.perform(basis.r)) {
				status[P_IMUATK][0] = (int) (imuatk.time * (1 + 0.2 / 3 * getFruit(atk.trait, atk.dire, -1)));
				status[P_IMUATK][1] = status[P_IMUATK][2] = imuatk.cd;
				anim.getEff(P_IMUATK);
			}
			if (status[P_IMUATK][0] > 0)
				return false;
		}
		if (getProc().IMUATKANY.exists()) { // TODO: 回避玉が果実または回避能力の補正を受けるか確認する
			if (status[P_IMUATK][0] == 0 && getProc().IMUATKANY.perform(basis.r)) {
				status[P_IMUATK][0] = 30;
				anim.getEff(P_IMUATK);
			}

			if (status[P_IMUATK][0] > 0)
				return false;
		}

		Proc.DMGCUT dmgcut = getProc().DMGCUT;

		if (dmgcut.exists() && ((dmgcut.type.traitIgnore && status[P_CURSE][0] == 0) || traitCompatible(atk.trait, atk.attacker, false))
				&& dmg < status[P_DMGCUT][0] && dmg > 0 && dmgcut.perform(basis.r)) {
			anim.getEff(P_DMGCUT);

			if (dmgcut.type.procs)
				proc = false;

			if (dmgcut.reduction == 100) {
				if (!proc)
					return false;
				dmg = 0;
			} else if (dmgcut.reduction != 0)
				dmg = dmg * (100 - dmgcut.reduction) / 100;
		}

		Proc.DMGCAP dmgcap = getProc().DMGCAP;

		if (dmgcap.exists() && ((dmgcap.type.traitIgnore && status[P_CURSE][0] == 0) || traitCompatible(atk.trait, atk.attacker, false)) && dmg > status[P_DMGCAP][0]
				&& dmgcap.perform(basis.r)) {
			anim.getEff(dmgcap.type.nullify ? DMGCAP_SUCCESS : DMGCAP_FAIL);
			if (dmgcap.type.procs)
				proc = false;

			if (dmgcap.type.nullify) {
				if (!proc)
					return false;
				dmg = 0;
			} else
				dmg = status[P_DMGCAP][0];
		}

		boolean barrierContinue = status[P_BARRIER][0] == 0;
		boolean shieldContinue = currentShield == 0;

		if (!barrierContinue) {
			if (atk.getProc().BREAK.prob > 0) {
				barrier.breakBarrier(true);
				barrierContinue = true;
			} else if (dmg >= status[P_BARRIER][0]) {
				barrier.breakBarrier(false);
				cancelAllProc();
			} else {
				anim.getEff(BREAK_NON);
				cancelAllProc();
			}
		}

		boolean metalKillerActivate = atk.getProc().METALKILL.mult > 0;

		if (dire == 1) {
			metalKillerActivate &= data.getTraits().contains(UserProfile.getBCData().traits.get(TRAIT_METAL));
		} else if (dire == -1) {
			metalKillerActivate &= (data.getAbi() & AB_METALIC) != 0;
		}

		if (metalKillerActivate) {
			dmg = dmg + (int) Math.max(health * atk.getProc().METALKILL.mult / 100f, 1f);
		}

		if (!shieldContinue) {
			if (atk.getProc().SHIELDBREAK.prob > 0) {
				currentShield = 0;
				anim.getEff(SHIELD_BREAKER);
				shieldContinue = true;
			} else if (dmg >= currentShield) {
				currentShield = 0;
				anim.getEff(SHIELD_BROKEN);
			} else {
				currentShield -= dmg;

				if (currentShield > maxCurrentShield)
					currentShield = maxCurrentShield;

				anim.getEff(SHIELD_HIT);
			}
		}

		if (!barrierContinue)
			return false;

		// -75は本家との比較から推測した表示位置
		if (atk.getProc().CRIT.mult > 0) {
			basis.lea.add(new EAnimCont(pos, currentLayer, effas().A_CRIT.getEAnim(DefEff.DEF), -75f));
			basis.leaSort = true;

			CommonStatic.setSE(SE_CRIT);
		}

		// -75は本家との比較から推測した表示位置
		if (atk.getProc().SATK.mult > 0) {
			basis.lea.add(new EAnimCont(pos, currentLayer, effas().A_SATK.getEAnim(DefEff.DEF), -75f));
			basis.leaSort = true;

			CommonStatic.setSE(SE_SATK);
		}

		if (metalKillerActivate) {
			basis.lea.add(new EAnimCont(pos, currentLayer, (dire == 1 ? effas().A_E_METAL_KILLER : effas().A_METAL_KILLER).getEAnim(DefEff.DEF), -75f));
			basis.leaSort = true;
		}

		if (!shieldContinue)
			return false;

		if ((atk.waveType & (WT_VOLC | WT_MIVC)) > 0) {
			if ((getAbi() & AB_CSUR) > 0 && atk instanceof AttackVolcano) {
				AttackVolcano volc = (AttackVolcano) atk;

				if (volc.handler != null && !volc.handler.reflected && !volc.handler.surgeSummoned.contains(this)) {
					basis.lea.add(new SurgeSummoner(pos, currentLayer, (dire == 1 ? effas().A_E_COUNTERSURGE : effas().A_COUNTERSURGE).getEAnim(DefEff.DEF),
							this, volc.handler.time, atk.waveType, volc.handler.startPoint,
							volc.handler.endPoint, 100));
					basis.leaSort = true;
					volc.handler.surgeSummoned.add(this);
				}
			}
		}

		if (!isBase)
			CommonStatic.setSE((basis.r.irDouble() < 0.5 ? SE_HIT_0 : SE_HIT_1));
		else if (basis.activeGuard != 1)
			CommonStatic.setSE(SE_HIT_BASE);

		damage += dmg;
		zx.damaged(atk);

		if (this instanceof EEnemy)
			status[P_BOUNTY][0] = atk.getProc().BOUNTY.mult;

		if (atk.atk < 0)
			anim.getEff(HEAL);

		// 煙の表示位置は本家との比較から推測
		if (atk.isLongAtk || atk instanceof AttackVolcano)
			anim.smoke = effas().A_WHITE_SMOKE.getEAnim(DefEff.DEF);
		else
			anim.smoke = effas().A_ATK_SMOKE.getEAnim(DefEff.DEF);

		anim.smokeLayer = (int) (currentLayer + 3 - basis.r.nextFloat() * -6);
		anim.smokeX = (int) (pos + 25 - basis.r.nextFloat() * -50);

		bondTree.damaged(atk, dmg, proc);

		final int FDmg = dmg;

		atk.notifyEntity(e -> {
			Proc.COUNTER counter = getProc().COUNTER;
			if (e.dire != dire && (e.touchable() & getTouch()) > 0 && counter.perform(basis.r)) {
				boolean isWave = (atk.waveType & WT_WAVE) > 0 || (atk.waveType & WT_MINI) > 0 || (atk.waveType & WT_MOVE) > 0 || (atk.waveType & WT_VOLC) > 0;
				if (!isWave || counter.type.counterWave != 0) {
					float[] ds = counter.minRange != 0 || counter.maxRange != 0 ? new float[]{pos + counter.minRange, pos + counter.maxRange} : aam.touchRange();
					int reflectAtk = FDmg;

					Proc reflectProc = Proc.blank();
					String[] par = { "CRIT", "KB", "WARP", "STOP", "SLOW", "WEAK", "POISON", "CURSE", "SNIPER", "VOLC", "MINIVOLC", "WAVE",
							"BOSS", "SEAL", "BREAK", "SUMMON", "SATK", "POIATK", "ARMOR", "SPEED", "LETHARGY", "SHIELDBREAK", "MINIWAVE",
							"DELAY" };

					if (counter.type.procType == 1 || counter.type.procType == 3)
						for (String s0 : par)
							if (s0.equals("VOLC") || s0.equals("WAVE") || s0.equals("MINIWAVE") || s0.equals("MINIVOLC")) {
								if (isWave && counter.type.counterWave == 2)
									reflectProc.get(s0).set(atk.getProc().get(s0));
							} else
								reflectProc.get(s0).set(atk.getProc().get(s0));

					if (data.getCounter() != null) {
						if (counter.type.useOwnDamage)
							reflectAtk = data.getCounter().atk;
						else
							reflectAtk = reflectAtk * counter.damage / 100;

						if (counter.type.procType >= 2) {
							Proc p = data.getCounter().getProc();
							for (String s0 : par)
								if (p.get(s0).perform(basis.r))
									reflectProc.get(s0).set(p.get(s0));
						}
					} else {
						if (counter.type.useOwnDamage)
							reflectAtk = getAtk();
						reflectAtk = reflectAtk * counter.damage / 100;

						if (counter.type.procType >= 2) {
							Proc p = data.getAllProc();
							for (String s0 : par) {
								if ((s0.equals("VOLC") || s0.equals("WAVE") || s0.equals("MINIWAVE")) && (!isWave || counter.type.counterWave != 2))
									continue;

								if (p.get(s0).perform(e.basis.r))
									reflectProc.get(s0).set(p.get(s0));
							}
						}
					}
					if (e.status[P_STRONG][0] != 0)
						reflectAtk += reflectAtk * e.status[P_STRONG][0] / 100;
					if (e.status[P_WEAK][0] > 0)
						reflectAtk = reflectAtk * e.status[P_WEAK][1] / 100;
					AttackSimple as = new AttackSimple(this, aam, reflectAtk, traits, getAbi(), reflectProc, ds[0], ds[1], e.data.getAtkModel(0), e.currentLayer, false);
					if (counter.type.areaAttack)
						as.capture();
					if (as.counterEntity(counter.type.outRange || (e.pos - ds[0]) * (e.pos - ds[1]) <= 0 ? e : null))
						anim.getEff(Data.P_COUNTER);
				}
			}

			int d = FDmg;

			if (status[P_ARMOR][0] > 0) {
				d = (int) (d * (100 + status[P_ARMOR][1]) / 100.0);
			}

			e.damageGiven += Math.min(d, health);

			if(e instanceof EUnit && ((EUnit) e).index != null) {
				int[] index = ((EUnit) e).index;

				basis.totalDamageGiven[index[0]][index[1]] += Math.min(d, health);
			}
		});

		if (proc)
			processProcs(atk);

		return true;
	}

	private int applyLethargy(int tba) {
		if (status[P_LETHARGY][0] > 0) {
			if (status[P_LETHARGY][2] == 0) {
				return tba + status[P_LETHARGY][1];
			} else if (status[P_LETHARGY][2] == 1) {
				return tba * (100 + status[P_LETHARGY][1]) / 100;
			} else if (status[P_LETHARGY][2] == 2) {
				return status[P_LETHARGY][1];
			}
		}
		return tba;
	}

	public boolean processProcs(AttackAb atk) {
		// 属性対象条件を満たす攻撃だけ能力を適用する
		if (!(traitCompatible(atk.trait, atk.attacker, false) || (receive(-1) && atk.SPtr) || (receive(1) && !atk.SPtr)))
			return false;

		boolean cannonResist = atk.canon > 0 && getProc().IMUCANNON.exists() && (atk.canon & getProc().IMUCANNON.type) > 0;
		int dire = data instanceof MaskEnemy ? 1 : -1;
		int trait = atk.attacker instanceof MaskUnit ? atk.attacker.data.getTraitsRaw().size() : 1;
		Proc atkProc = atk.getProc();
		if (atkProc.POIATK.mult > 0) {
			int rst = getProc().IMUPOIATK.mult;

			if (rst == 100) {
				anim.getEff(INV);
			} else {
				float poiDmg = atkProc.POIATK.mult * (100 - rst) / 10000f;

				if (this.dire == -1 && basis.canon.deco == DECO_BASE_BARRIER)
					poiDmg *= basis.b.t().getDecorationMagnification(basis.canon.deco, Data.DECO_TOXIC);

				damage = (long) (damage + maxH * poiDmg);

				basis.lea.add(new EAnimCont(pos, currentLayer, effas().A_POISON.getEAnim(DefEff.DEF)));
				basis.leaSort = true;

				CommonStatic.setSE(SE_POISON);
			}
		}

		float f = getFruit(atk.trait, atk.dire, 1);
		float time = atk.origin instanceof AttackCanon ? 1 : 1 + f * 0.2f / 3;
		float dist = 1 + f * 0.1f;

		if (atkProc.STOP.time != 0 || atkProc.STOP.prob > 0) {
			int val = (int) (atkProc.STOP.time * time);
			float rst = getResistValue(atk, "IMUSTOP", getProc().IMUSTOP.mult);

			if (rst > 0f) {
				val = (int) (val * rst);

				if (val < 0)
					status[P_STOP][0] = Math.max(status[P_STOP][0], Math.abs(val));
				else
					status[P_STOP][0] = val;

				anim.getEff(P_STOP);
				basis.scoreActivated(SCORE_STOP, dire, trait);
			} else
				anim.getEff(INV);

			if(this.dire == -1 && basis.canon.deco == DECO_BASE_STOP) {
				status[P_STOP][0] = (int) (status[P_STOP][0] * basis.b.t().getDecorationMagnification(basis.canon.deco, Data.DECO_FREEZE));
			}
		}

		if (atkProc.SLOW.time != 0 || atkProc.SLOW.prob > 0) {
			int val = (int) (atkProc.SLOW.time * time);
			float rst = getResistValue(atk, "IMUSLOW", getProc().IMUSLOW.mult);

			if (rst > 0f) {
				val = (int) (val * rst);

				if (val < 0)
					status[P_SLOW][0] = Math.max(status[P_SLOW][0], Math.abs(val));
				else
					status[P_SLOW][0] = val;

				anim.getEff(P_SLOW);
				basis.scoreActivated(SCORE_SLOW, dire, trait);
			} else
				anim.getEff(INV);

			if(this.dire == -1 && basis.canon.deco == DECO_BASE_SLOW) {
				status[P_SLOW][0] = (int) (status[P_SLOW][0] * basis.b.t().getDecorationMagnification(basis.canon.deco, Data.DECO_SLOW));
			}
		}

		if (atkProc.WEAK.time > 0) {
			int val = (int) (atkProc.WEAK.time * time);
			float rst = getResistValue(
					atk,
					"IMUWEAK",
					Proc.checkSmartImu(atkProc.WEAK.mult - 100, getProc().IMUWEAK.smartImu, getProc().IMUWEAK.mult > 0) ? getProc().IMUWEAK.mult : 0
			);

			val = (int) (val * rst);
			if(this.dire == -1 && basis.canon.deco == DECO_BASE_GROUND) {
				val = (int) (val * basis.b.t().getDecorationMagnification(basis.canon.deco, Data.DECO_WEAK));
			}

			if (rst > 0f) {
				weaks.add(new int[] { val, atkProc.WEAK.mult });
				anim.getEff(P_WEAK);
				basis.scoreActivated(SCORE_WEAK, dire, trait);
			} else
				anim.getEff(INV);
		}

		if (atkProc.CURSE.time != 0 || atkProc.CURSE.prob > 0) {
			int val = (int) (atkProc.CURSE.time * time);
			float rst = getResistValue(atk, "IMUCURSE", getProc().IMUCURSE.mult);

			if (rst > 0f) {
				val = (int) (val * rst);
				if (val < 0)
					status[P_CURSE][0] = Math.max(status[P_CURSE][0], Math.abs(val));
				else
					status[P_CURSE][0] = val;

				anim.getEff(P_CURSE);
			} else
				anim.getEff(INV);

			if(this.dire == -1 && basis.canon.deco == DECO_BASE_CURSE) {
				status[P_CURSE][0] = (int) (status[P_CURSE][0] * basis.b.t().getDecorationMagnification(basis.canon.deco, Data.DECO_CURSE));
			}
		}

		if (atkProc.KB.dis != 0) {
			float rst = getResistValue(atk, "IMUKB", getProc().IMUKB.mult);

			if (rst > 0f) {
				status[P_KB][0] = atkProc.KB.time;

				interrupt(P_KB, atkProc.KB.dis * dist * rst);
				basis.scoreActivated(SCORE_KB, dire, trait);
			} else
				anim.getEff(INV);
		}

		if (atkProc.SNIPER.prob > 0)
			interrupt(INT_ASS, KB_DIS[INT_ASS]);

		if (atkProc.BOSS.prob > 0)
			interrupt(INT_SW, KB_DIS[INT_SW]);

		if (atkProc.WARP.exists())
			if (getProc().IMUWARP.mult < 100) {
				Proc.WARP warp = atkProc.WARP;

				interrupt(INT_WARP, warp.dis_0 + (int) (basis.r.nextFloat() * (warp.dis_1 - warp.dis_0)));

				EffAnim<WarpEff> e = effas().A_W;

				int len = e.len(WarpEff.ENTER) + e.len(WarpEff.EXIT);
				int val = (int) (warp.time * time);
				float rst = getResistValue(atk, "IMUWARP", getProc().IMUWARP.mult);
				val = (int) (val * rst);

				status[P_WARP][0] = val + len;
			} else
				anim.getEff(INVWARP);

		if (atkProc.SEAL.prob > 0) {
			int rst = data.getProc().IMUSEAL.mult;

			if (rst < 100) {
				int val = (int) (atkProc.SEAL.time * time);

				val = val * (100 - rst) / 100;

				if (val < 0)
					status[P_SEAL][0] = Math.max(status[P_SEAL][0], Math.abs(val));
				else
					status[P_SEAL][0] = val;

				anim.getEff(P_SEAL);
			} else
				anim.getEff(INV);
		}

		if (atkProc.POISON.time > 0) {
			int res = Proc.checkSmartImu(atkProc.POISON.damage, getProc().IMUPOI.smartImu, getProc().IMUPOI.mult < 0) ? getProc().IMUPOI.mult : 0;

			if (res < 100) {
				POISON ws = (POISON) atkProc.POISON.clone();

				ws.time = ws.time * (100 - res) / 100;

				if (atk.atk != 0 && ws.type.modifAffected)
					ws.damage = (int) (ws.damage * (float) getDamage(atk, atk.atk) / atk.atk);

				pois.add(ws);
				anim.getEff(P_POISON);
			} else
				anim.getEff(INV);
		}

		if (!isBase && atkProc.ARMOR.time > 0) {
			int res = Proc.checkSmartImu(atkProc.ARMOR.mult, getProc().IMUARMOR.smartImu, getProc().IMUARMOR.mult < 0) ? getProc().IMUARMOR.mult : 0;

			if (res < 100) {
				int val = (int) (atkProc.ARMOR.time * time);
				status[P_ARMOR][0] = val * (100 - res) / 100;
				status[P_ARMOR][1] = atkProc.ARMOR.mult;

				anim.getEff(P_ARMOR);
			} else
				anim.getEff(INV);
		}

		if (atkProc.SPEED.time > 0) {
			int res = getProc().IMUSPEED.mult;
			int speed = data.getSpeed();
			if (speed > 0 && basis.getGlobalSpeed(this.dire, speed) > 0)
				speed = basis.getGlobalSpeed(this.dire, speed);

			boolean b;

			if (atkProc.SPEED.type == 2)
				b = (speed > atkProc.SPEED.speed && res > 0) || (speed < atkProc.SPEED.speed && res < 0);
			else
				b = res < 0;
			if (Proc.checkSmartImu(atkProc.SPEED.speed, getProc().IMUSPEED.smartImu, b))
				res = 0;

			if (res < 100) {
				int val = (int) (atkProc.SPEED.time * time);
				status[P_SPEED][0] = val * (100 - res) / 100;
				status[P_SPEED][1] = atkProc.SPEED.speed;
				status[P_SPEED][2] = atkProc.SPEED.type;

				anim.getEff(P_SPEED);
			} else
				anim.getEff(INV);
		}

		if (atkProc.LETHARGY.time > 0) {
			int res = getProc().IMULETH.mult;
			int tba = data.getTBA();

			boolean isBuff; // 設定TBAと元TBAの大小で強化・弱体化の表示を選ぶ

			if (atkProc.LETHARGY.type == 2)
				isBuff = (tba > atkProc.LETHARGY.mult && res > 0) || (tba < atkProc.LETHARGY.mult && res < 0);
			else
				isBuff = res < 0;
			if (Proc.checkSmartImu(atkProc.LETHARGY.mult, getProc().IMULETH.smartImu, !isBuff))
				res = 0;

			if (res < 100) {
				int val = (int) (atkProc.LETHARGY.time * time);
				status[P_LETHARGY][0] = val * (100 - res) / 100;
				status[P_LETHARGY][1] = atkProc.LETHARGY.mult;
				status[P_LETHARGY][2] = atkProc.LETHARGY.type;

				anim.getEff(P_LETHARGY);
			} else
				anim.getEff(INV);
		}
		return true;
	}

	public abstract float getResistValue(AttackAb atk, String procName, int procResist);

	/**
	 * 封印中に残る例外能力を考慮した現在の能力ビット列を返す。
	 */
	@Override
	public int getAbi() {
		if (status[P_SEAL][0] > 0)
			return (data.getAbi() ^ altAbi) & (AB_ONLY | AB_METALIC | AB_GLASS);
		return data.getAbi() ^ altAbi;
	}

	/**
	 * 表示と反撃計算に使う現在の攻撃力を返す。
	 */
	public int getAtk() {
		return aam.getAtk();
	}

	/**
	 * 封印状態を考慮した現在の能力集合を返す。
	 */
	public Proc getProc() {
		if (status[P_SEAL][0] > 0)
			return sealed;
		return proc;
	}

	/**
	 * 優先度判定対象として割り込みを予約する。
	 */
	public void interrupt(int t, float d) {
		if(isBase && health <= 0)
			return;

		kb.interrupt(t, d);
	}

	@Override
	public boolean isBase() {
		return isBase;
	}

	/**
	 * 死亡状態へ移行して攻撃を停止し、死亡演出を開始する。
	 *
	 * @param atk 死亡理由。通常撃破以外は撃破報酬の対象外
	 */
	public void kill(KillMode atk) {
		if (kbTime == -1)
			return;
		kbTime = -1;
		atkm.stopAtk();
		anim.kill();
		basis.checkGuard();
		if (atk == KillMode.NORMAL)
			for (AttackAb attack : lastKilledBy)
				if (attack.attacker != null)
					attack.attacker.killCount++;
	}

	/**
	 * コンティニュー時に進行中の攻撃とノックバック演出を打ち切る。
	 */
	public void cont() {
		atkm.stopAtk();
		anim.cont();
	}

	/**
	 * 蓄積ダメージと能力を確定し、割り込み・死亡・撃破元集計を更新する。
	 */
	@Override
	public void postUpdate() {
		regenUpdate();

		int hb = data.getHb();
		long ext = health * hb % maxH;
		if (ext == 0)
			ext = maxH;
		if (status[P_ARMOR][0] > 0) {
			damage = (long) (damage * (100 + status[P_ARMOR][1]) / 100.0);
		}
		if (!isBase && damage > 0 && kbTime <= 0 && kbTime != -1 && (ext <= damage * hb || health < damage))
			interrupt(INT_HB, KB_DIS[INT_HB]);
		if (damage > 0 && isBase && basis.activeGuard == 1) {
			anim.getEff(GUARD_HOLD);
		} else {
			health -= damage;
		}

		if (health > maxH)
			health = maxH;
		damage = 0;

		// 体力低下による攻撃力上昇
		int strongThreshold = getProc().STRONG.health;
		if ((touchable() & TCH_CORPSE) == 0 && status[P_STRONG][0] == 0 && strongThreshold > 0 && health * 100 <= maxH * strongThreshold) {
			status[P_STRONG][0] = getProc().STRONG.mult;
			anim.getEff(P_STRONG);
		}
		// 撃破数による攻撃力上昇
		int requiredKills = getProc().BERSERK.killCount;
		if ((touchable() & TCH_CORPSE) == 0 && status[P_STRONG][1] == 0 && requiredKills > 0 && killCount >= requiredKills) {
			status[P_STRONG][1] = getProc().BERSERK.mult;
			anim.getEff(P_STRONG);
		}
		// 体力低下による速度上昇
		int adrenalineThreshold = getProc().SPEEDUP.health;
		if (status[P_SPEEDUP][0] == 0 && (touchable() & TCH_CORPSE) == 0 && adrenalineThreshold > 0 && health * 100 <= maxH * adrenalineThreshold) {
			status[P_SPEEDUP][0] = getProc().SPEEDUP.mult;
			anim.getEff(P_SPEEDUP);
		}
		// 生き残る
		if (getProc().LETHAL.prob > 0 && health <= 0) {
			if (status[P_LETHAL][0] == 0 && getProc().LETHAL.perform(basis.r)) {
				health = 1;
				anim.getEff(P_LETHAL);
			}
			status[P_LETHAL][0]++;
		}

		for (int i = 0; i < tokens.size(); i++)
			tokens.get(i).model.invokeLater(tokens.get(i), this);
		tokens.clear();

		if(isBase && health <= 0)
			kbTime = 1;

		kb.doInterrupt();

		if ((getAbi() & AB_GLASS) > 0 && atkm.atkTime - 1 <= 0 && kbTime == 0 && atkm.attacksLeft == 0)
			kill(KillMode.SELF_DESTRUCT);

		// ゾンビキラーの一時状態を更新する
		zx.postUpdate();

		if (isBase && health < 0) {
			health = 0;
			atkm.stopAtk();
			anim.setAnim(UType.HB, true);
		}

		if(!dead || !summoned.isEmpty()) {
			livingTime++;
		}

		summoned.removeIf(s -> !s.activate);

		if (health <= 0 && zx.canRevive() == 0 && !killCounted) {
			onLastBreathe();
			killCounted = true;
			lastKilledBy.addAll(lastHitBy);
		}

		lastHitBy.clear();
	}

	/**
	 * 召喚時の登場状態を設定し、必要なら召喚元とHP共有関係を結ぶ。
	 * @param conf 登場演出の種別
	 */
	public void setSummon(int conf, Entity bond) {

		if (conf == 1) {
			kb.kbType = INT_WARP;
			kbTime = effas().A_W.len(WarpEff.EXIT);
			status[P_WARP][2] = 1;
		}

		if (conf == 2 && data.getPack().anim.anims.length >= 7) {
			kbTime = -3;
			bdist = -1;
		}

		if (conf == 3 && data.getPack().anim.anims.length >= 7) {
			status[P_BURROW] = new int[PROC_WIDTH];
			kbTime = -3;
			bdist = -1;
		}

		if (bond != null) {
			bond.bondTree.children.add(this);
			bondTree.children.add(bond);
		}
	}

	/**
	 * 攻撃側と共有属性または対象属性条件があるかを判定する。
	 * @param t 攻撃側の属性一覧
	 * @param attacker 攻撃元
	 * @param targetOnly 対象限定判定として呼ぶ場合は真
	 */
	@Override
	public boolean traitCompatible(List<Trait> t, Entity attacker, boolean targetOnly) {
		if (targetOnly && isBase) return true;
		if (t.contains(null))
			return true;
		for (Trait trait : t)
			if (traits.contains(trait))
				return true;
		if (Trait.isTargetTraited(traits))
			for (int i = 0; i < t.size(); i++)
				if (t.get(i).targetType)
					return true;
		return false;
	}

	/**
	 * 死亡・復活・潜行・登場状態に対応する接触種別を返す。
	 */
	@Override
	public int touchable() {
		int n = (getAbi() & AB_GHOST) > 0 ? TCH_EX : TCH_N;
		int ex = getProc().REVIVE.type.revive_others ? TCH_ZOMBX : 0;
		if (kbTime == -1 && anim.soul != null)
			return TCH_SOUL | ex;
		if (status[P_REVIVE][1] >= REVIVE_SHOW_TIME && anim.corpse != null && anim.corpse.type != ZombieEff.BACK)
			return TCH_CORPSE | ex;
		if (kbTime == -2 || kbTime == -4)
			return n | TCH_UG | ex;
		if (kbTime == -3)
			return TCH_UG | ex;
		if (anim.anim.type == UType.ENTER)
			return TCH_ENTER | ex;
		return (kbTime == 0 ? n : TCH_KB) | ex;
	}

	/**
	 * 弱体・毒の内部トークンを破棄し、解除対象状態を終了直前へ進める。
	 */
	private void cancelAllProc() {
		weaks.list.clear();
		pois.list.clear();

		for (int i = 0; i < REMOVABLE_PROC.length; i++)
			if (status[REMOVABLE_PROC[i]][0] > 0)
				status[REMOVABLE_PROC[i]][0] = 1;
	}

	private boolean walking = true;
	private boolean regenDisabled = false;

	private void regenerate() {
		int amount = status[P_HPREGEN][2];
		if (amount < 0 && !getProc().HPREGEN.noHB) { // この扱いで正しいか未確認
			damage -= amount;
		} else {
			health += amount;
		}
		if (getProc().HPREGEN.removeProcs)
			cancelAllProc();
		if (health < 1) {
			regenDisabled = true;
			if (getProc().HPREGEN.noHB)
				preKill();
		}
	}

	private void regenUpdate() {
		if (getProc().HPREGEN.prob > 0 && !regenDisabled) {
			if (regentimer > 0) {
				if (((getProc().HPREGEN.idleTrigger && !walking && !dead) || !getProc().HPREGEN.idleTrigger) && ((getProc().HPREGEN.freezeEff && status[P_STOP][0] == 0) || !getProc().HPREGEN.freezeEff)) {
					regentimer--;
				}
				if (regentimer < 1) {
					if (getProc().HPREGEN.onlyOnce) {
						regenDisabled = true;
					}
					regentimer = getProc().HPREGEN.interval;
					if (basis.r.nextFloat() < (getProc().HPREGEN.prob / 100f)) {
						regenerate();
					}
				}
			}
		}
	}

	/**
	 * 第1更新として、待機時間、能力時間、割り込み・潜行・通常移動、復活の順に進行する。
	 * 第2更新は {@link #update2()} が接触反応と攻撃を処理する。
	 */
	@Override
	public void update() {
		// 攻撃待機時間を減らす
		if (waitTime > 0)
			waitTime--;

		updateProc();
		barrier.update();

		if (kbTime > 0)
			kb.updateKB();
		else if (status[P_STOP][0] == 0) {
			if (kbTime < -1)
				updateBurrow();
			else if (kbTime == 0 && walking && !checkTouch())
				updateMove(0);
		}

		zx.updateRevive();
	}

	@Override
	public void update2() {
		// 反応不能状態では演出と共有HP関係だけを更新する
		if (kbTime != 0 || (isBase && health <= 0) || anim.anim.type == UType.ENTER || status[P_REVIVE][1] != 0) {
			anim.update();
			bondTree.update();
			return;
		}

		// 停止中も接触に応じて演出状態を変え、停止が切れたフレームから移動できる
		boolean nstop = status[P_STOP][0] == 0;

		// 非攻撃中は接触状態から歩行・潜行・攻撃開始を選ぶ
		if(atkm.atkTime == 0) {
			if (checkTouch()) {
				walking = false;
				anim.setAnim(UType.IDLE, true);
				if(nstop) {
					if (status[P_BURROW][0] != 0 && !skipSpawnBurrow && (basis.getBase(dire).pos - pos) * dire > data.touchBase())
						startBurrow();
					else if (waitTime == 0 && touchEnemy && atkm.attacksLeft != 0)
						atkm.startAttack();
				}
			} else {
				walking = true;
				anim.setAnim(UType.WALK, true);
			}
		}

		if (atkm.atkTime > 0 && nstop)
			atkm.updateAttack();

		anim.update();
		bondTree.update();
	}

	@Override
	public void updateAnimation() {
		anim.updateAnimation();
	}

	protected int critCalc(boolean isMetal, int ans, AttackAb atk) {
		int satk = atk.getProc().SATK.mult;
		if (satk > 0)
			ans = (int) (ans * (100 + satk) * 0.01);
		int crit = atk.getProc().CRIT.mult;
		int criti = getProc().CRITI.mult;
		if (criti == 100)
			crit = 0;
		else if (criti != 0)
			crit = (int) (crit * (100 - getProc().CRITI.mult) / 100.0);
		if (isMetal)
			if (crit > 0)
				ans = (int) (ans * 0.01 * crit);
			else if (crit < 0)
				ans = (int) Math.max(1, health * crit / -100);
			else
				ans = ans > 0 ? 1 : 0;
		else if (crit > 0)
			ans = (int) (ans * 0.01 * crit);
		else if (crit < 0)
			ans = (int) Math.max(1, health / 1000);
		return ans;
	}

	/**
	 * 陣営固有の特性補正を適用した被ダメージを返す。
	 */
	protected abstract int getDamage(AttackAb atk, int ans);

	/**
	 * 復活・生き残るがない最終死亡の確定時に呼ばれる。
	 */
	protected abstract void onLastBreathe();

	/**
	 * 後退可能な最大距離を返す。
	 */
	protected abstract float getLim();

	protected abstract int traitType();

	/**
	 * 状態速度と追加移動量を反映して前進する。
	 * @param extmov コンボなどによる追加移動量
	 */
	protected void updateMove(float extmov) {
		if (moved)
			skipSpawnBurrow = false;
		moved = true;

		if(status[P_SLOW][0] > 0) {

			pos += 0.25f * dire;

		} else {
			int speed = data.getSpeed();
			if (speed > 0 && basis.getGlobalSpeed(dire, speed) > -1)
				speed = basis.getGlobalSpeed(dire, speed);
			float mov = speed * 0.5f;

			if (status[P_SPEED][0] > 0) {
				if (status[P_SPEED][2] == 0) {
					mov += status[P_SPEED][1] * 0.5f;
				} else if (status[P_SPEED][2] == 1) {
					mov = mov * (100 + status[P_SPEED][1]) / 100;
				} else if (status[P_SPEED][2] == 2) {
					mov = status[P_SPEED][1] * 0.5f;
				}
			}

			if (status[P_SPEEDUP][0] != 0) {
				mov += (mov * status[P_SPEEDUP][0] / 100f);
				mov = (float) Math.round(mov * 4f) / 4f;
			}

			pos += (mov + extmov) * dire;

		}

		if (kbTime == 0)
			lastPosition = pos;

	}

	private void drawAxis(FakeGraphics gra, P p, float siz) {
		// 以降は当たり判定のデバッグ描画
		siz *= 1.25f;
		float rat = BattleConst.ratio;
		float poa = p.x - pos * rat * siz;
		int py = (int) p.y;
		int h = (int) (640 * rat * siz);
		gra.setColor(FakeGraphics.RED);
		for (int i = 0; i < data.getAtkCount(); i++) {
			float[] ds = aam.inRange(i);
			float d0 = Math.min(ds[0], ds[1]);
			float ra = Math.abs(ds[0] - ds[1]);
			int x = (int) (d0 * rat * siz + poa);
			int y = (int) (p.y + 100 * i * rat * siz);
			int w = (int) (ra * rat * siz);
			if (atkm.tempAtk == i)
				gra.fillRect(x, y, w, h);
			else
				gra.drawRect(x, y, w, h);
		}
		gra.setColor(FakeGraphics.YELLOW);
		int x = (int) ((pos + data.getRange() * dire) * rat * siz + poa);
		gra.drawLine(x, py, x, py + h);
		gra.setColor(FakeGraphics.BLUE);
		int bx = (int) ((dire == -1 ? pos : pos - data.getWidth()) * rat * siz + poa);
		int bw = (int) (data.getWidth() * rat * siz);
		gra.drawRect(bx, (int) p.y, bw, h);
		gra.setColor(FakeGraphics.CYAN);
		gra.drawLine((int) (pos * rat * siz + poa), py, (int) (pos * rat * siz + poa), py + h);
		atkm.tempAtk = -1;
	}

	/**
	 * 敵が受ける妨害時間への果実補正を返す。
	 */
	private float getFruit(List<Trait> trait, int dire, int e) {
		if (!receive(dire) || receive(e))
			return 0;
		ArrayList<Trait> sharedTraits = new ArrayList<>(trait);
		sharedTraits.retainAll(traits);
		return basis.b.t().getFruit(sharedTraits);
	}

	/**
	 * 最終ノックバック後に復活可否を判定し、死亡を確定する。
	 */
	private void preKill() {
		Soul s = Identifier.get(data.getDeathAnim());
		if (s != null && s.audio != null)
			CommonStatic.setSE(s.audio);
		else
			CommonStatic.setSE(basis.r.irDouble() < 0.5 ? SE_DEATH_0 : SE_DEATH_1);

		if (zx.prekill())
			return;

		kill(KillMode.NORMAL);
	}

	/**
	 * 能力補正の攻撃側・防御側判定を返す。
	 */
	private boolean receive(int dire) {
		return traitType() != dire;
	}

	private void startBurrow() {
		status[P_BURROW][0]--;
		status[P_BURROW][2] = anim.setAnim(UType.BURROW_DOWN, false);
		kbTime = -2;
	}

	private void updateBurrow() {
		if (kbTime == -2) {
			// 潜行開始
			status[P_BURROW][2]--;
			if (data.getGouge() != null && anim.anim.len() - status[P_BURROW][2] == data.getGouge().pre)
				basis.getAttack(aam.getAttack(data.getAtkCount() + 2));
			if (status[P_BURROW][2] == 0) {
				bdist = data.getRepAtk().getProc().BURROW.dis;
				anim.setAnim(UType.BURROW_MOVE, true);
				kbTime = -3;
			}
		}
		if (kbTime == -3) {
			// 地中移動
			float oripos = pos;
			if (bdist < 0) {
				status[P_BURROW][2] = anim.setAnim(UType.BURROW_UP, false) + 1;
				kbTime = -4;
			} else if ((basis.getBase(dire).pos - pos) * dire - data.touchBase() <= 0) {
				status[P_BURROW][2] = anim.setAnim(UType.BURROW_UP, true);
				kbTime = -4;
			} else {
				updateMove(0);
				bdist -= (pos - oripos) * dire;
			}
		}
		if (kbTime == -4) {
			// 浮上
			status[P_BURROW][2]--;
			if (data.getResurface() != null && anim.anim.len() - status[P_BURROW][2] == data.getResurface().pre)
				basis.getAttack(aam.getAttack(data.getAtkCount() + 3));
			if (status[P_BURROW][2] == 0)
				kbTime = 0;
		}

	}

	/**
	 * 時間制能力と内部トークンを1フレーム進める。
	 */
	private void updateProc() {
		if (status[P_STOP][0] > 0)
			status[P_STOP][0]--;
		if (status[P_SLOW][0] > 0)
			status[P_SLOW][0]--;
		if (status[P_CURSE][0] > 0)
			status[P_CURSE][0]--;
		if (status[P_SEAL][0] > 0)
			status[P_SEAL][0]--;
		if (status[P_IMUATK][0] > 0)
			status[P_IMUATK][0]--;
		else if (status[P_IMUATK][1] > 0) {
			if (status[P_IMUATK][1] == status[P_IMUATK][2]) {
				anim.getEff(IMUATK_CD);
			}
			status[P_IMUATK][1]--;
		}
		if (status[P_ARMOR][0] > 0)
			status[P_ARMOR][0]--;
		if (status[P_SPEED][0] > 0)
			status[P_SPEED][0]--;
		if (status[P_LETHARGY][0] > 0)
			status[P_LETHARGY][0]--;
		if (status[P_BSTHUNT][0] > 0)
			status[P_BSTHUNT][0]--;
		else if (status[P_BSTHUNT][1] > 0) {
			if (status[P_BSTHUNT][1] == status[P_BSTHUNT][2]) {
				anim.getEff(IMUATK_CD);
			}
			status[P_BSTHUNT][1]--;
		}
		// 複数同時適用される弱体・毒を更新する
		weaks.update();
		pois.update();
	}

	/**
	 * 死体攻撃能力を加味した接触対象ビット列を返す。
	 */
	public int getTouch() {
		if ((getAbi() & AB_CKILL) > 0)
			return data.getTouch() | TCH_CORPSE;
		return data.getTouch();
	}

	/**
	 * 接触範囲内の対象と対象限定条件を更新し、移動停止要否を返す。
	 */
	public boolean checkTouch() {
		touch = true;
		float[] ds = aam.touchRange();
		List<AbEntity> le = basis.inRange(getTouch(), -dire, ds[0], ds[1], false);
		boolean blds;
		if (data.isLD() || data.isOmni()) {
			float bpos = basis.getBase(dire).pos;
			blds = (bpos - pos) * dire > data.touchBase();
			if (blds)
				le.remove(basis.getBase(dire));
			if (dire == -1 && pos <= bpos && !le.contains(basis.getBase(dire)))
				le.add(basis.getBase(dire));
			else if(dire == 1 && pos >= bpos && !le.contains(basis.getBase(dire)))
				le.add(basis.getBase(dire));
			blds &= le.isEmpty();
		} else {
			blds = le.isEmpty();
		}
		if (blds)
			touch = false;
		touchEnemy = touch;
		if ((getAbi() & AB_ONLY) > 0) {
			touchEnemy = false;
			for (int i = 0; i < le.size(); i++)
				if (le.get(i).traitCompatible(traits, this, true))
					touchEnemy = true;
		}
		return touch;
	}

	public void setWaitTime(int t) {
		waitTime = t;
	}
}
