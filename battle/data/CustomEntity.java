package common.battle.data;

import common.io.json.JsonClass;
import common.io.json.JsonClass.NoTag;
import common.io.json.JsonDecoder;
import common.io.json.JsonField;
import common.io.json.JsonField.GenType;
import common.pack.Identifier;
import common.util.Data;
import common.util.unit.AbEnemy;
import common.util.unit.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * カスタムキャラクターの通常攻撃配列、代表能力、特殊攻撃を管理する基底データ。
 * {@code common} が偽の場合は攻撃ごとの能力を集約し、特殊攻撃は通常攻撃数以降の固定順で参照する。
 */
@JsonClass(noTag = NoTag.LOAD)
public abstract class CustomEntity extends DataEntity {

	public static int SPECIAL_ATTACK_COUNT = 7;

	@JsonField(gen = GenType.GEN)
	public AtkDataModel rev, res, bur, resu, revi, glas, cntr; // 特殊攻撃

	@JsonField(gen = GenType.GEN)
	public AtkDataModel rep; // 共通能力の代表攻撃

	@JsonField(gen = GenType.GEN, usePool = true)
	public AtkDataModel[] atks;

	public int tba, base, touch = TCH_N;
	public boolean common = true, kbBounce = true, bossBounce = true;

	/**
	 * {@code common} が偽の場合の全攻撃能力と、反撃で参照する能力の集約先。
	 */
	@JsonField(block = true)
	private Proc all;

	@Override
	public int allAtk() {
		int ans = 0, temp = 0, c = 1;
		for (AtkDataModel adm : atks)
			if (adm.pre > 0) {
				ans += temp / c;
				temp = adm.getDire() > 0 ? adm.atk : 0;
				c = 1;
			} else {
				temp += adm.getDire() > 0 ? adm.atk : 0;
				c++;
			}
		ans += temp / c;
		return ans;
	}

	/**
	 * 攻撃ごとの能力から集約値を再構築する。
	 */
	public void updateAllProc() {
		all = Proc.blank();
		for (int i = 0; i < Data.PROC_TOT; i++) {
			if (Data.procSharable[i]) {
				all.getArr(i).set(getProc().getArr(i));
			} else
				for (AtkDataModel adm : atks)
					if (!all.getArr(i).exists())
						all.getArr(i).set(adm.proc.getArr(i));
		}
	}

	/**
	 * 共通能力を使わないキャラクターでは、攻撃ごとの能力を集約して返す。
	 */
	@Override
	public Proc getAllProc() {
		if (common)
			return getProc();
		if (all == null)
			updateAllProc();
		return all;
	}

	@Override
	public int getAtkCount() {
		return atks.length;
	}

	@Override
	public MaskAtk getAtkModel(int ind) {
		if (ind < atks.length)
			return atks[ind];
		if (ind == atks.length)
			return rev;
		if (ind == atks.length + 1)
			return res;
		if (ind == atks.length + 2)
			return bur;
		if (ind == atks.length + 3)
			return resu;
		if (ind == atks.length + 4)
			return revi;
		if (ind == atks.length + 5)
			return glas;
		if (ind == atks.length + 6)
			return cntr;

		return null;
	}

	@Override
	public MaskAtk[] getAtks() {
		return atks;
	}

	public String getAvailable(String str) {
		while (contains(str))
			str += "'";

		return str;
	}

	@Override
	public int getItv() {
		int longPre = 0;
		for (AtkDataModel adm : atks)
			longPre += adm.pre;
		return longPre + Math.max(getTBA() - 1, getPost());
	}

	@Override
	public int getPost() {
		int ans = getAnimLen();
		for (AtkDataModel adm : atks)
			ans -= adm.pre;
		return ans;
	}

	@Override
	public Proc getProc() {
		return rep.getProc();
	}

	@Override
	public MaskAtk getRepAtk() {
		return rep;
	}

	@Override
	public AtkDataModel getResurrection() {
		return res;
	}

	@Override
	public AtkDataModel getRevenge() {
		return rev;
	}

	@Override
	public AtkDataModel getCounter() { return cntr; }

	@Override
	public AtkDataModel getGouge() {
		return bur;
	}

	@Override
	public AtkDataModel getResurface() {
		return resu;
	}

	@Override
	public AtkDataModel getRevive() {
		return revi;
	}

	@Override
	public AtkDataModel getGlass() {
		return glas;
	}

	@Override
	public int getTBA() {
		return Math.abs(tba);
	}
	@Override
	public int getRealTBA() {
		return tba;
	}

	@Override
	public int getTouch() {
		return touch;
	}

	public void importData(MaskEntity src) {
		hp = src.getHp();
		hb = src.getHb();
		speed = src.getSpeed();
		range = src.getRange();
		abi = src.getAbi();
		loop = src.getAtkLoop();
		width = src.getWidth();
		tba = src.getTBA();
		touch = src.getTouch();
		death = src.getDeathAnim();
		will = src.getWill();
		if (src instanceof CustomEntity) {
			importData$1((CustomEntity) src);
			return;
		}

		base = src.touchBase();
		common = ((DefaultData)src).isCommon();
		kbBounce = true;
		bossBounce = true;
		rep = new AtkDataModel(this);
		rep.proc = src.getRepAtk().getProc().clone();
		int m = src.getAtkCount();
		atks = new AtkDataModel[m];
		for (int i = 0; i < m; i++) {
			atks[i] = new AtkDataModel(this, src, i);
			for (int j : BCShareable)
				atks[i].proc.getArr(j).set(src.getProc().getArr(j));
		}
	}

	@Override
	public boolean isLD() {
		boolean ans = false;
		for (AtkDataModel adm : atks)
			ans |= adm.isLD();
		if (getRevenge() != null)
			ans |= getRevenge().isLD();
		if (getResurrection() != null)
			ans |= getResurrection().isLD();
		if (getGouge() != null)
			ans |= getGouge().isLD();
		if (getResurface() != null)
			ans |= getResurface().isLD();
		if (getRevive() != null)
			ans |= getRevive().isLD();
		if (getGlass() != null)
			ans |= getGlass().isLD();
		return ans;
	}

	/**
	 * 指定攻撃だけが遠方攻撃かを返す。
	 * @param ind 攻撃の添字
	 */
	@Override
	public boolean isLD(int ind) {
		AtkDataModel model = (AtkDataModel) getAtkModel(ind);
		return model.isLD();
	}

	@Override
	public boolean isOmni() {
		boolean ans = false;
		for (AtkDataModel adm : atks)
			ans |= adm.isOmni();
		if(getRevenge() != null)
			ans |= getRevenge().isOmni();
		if(getResurrection() != null)
			ans |= getResurrection().isOmni();
		if(getGouge() != null)
			ans |= getGouge().isOmni();
		if(getResurface() != null)
			ans |= getResurface().isOmni();
		if(getRevive() != null)
			ans |= getRevive().isOmni();
		if (getGlass() != null)
			ans |= getGlass().isOmni();
		return ans;
	}

	/**
	 * 指定攻撃だけが全方位攻撃かを返す。
	 * @param ind 攻撃の添字
	 */
	@Override
	public boolean isOmni(int ind) {
		AtkDataModel model = (AtkDataModel) getAtkModel(ind);
		return model.isOmni();
	}

	@Override
	public boolean isRange() {
		for (AtkDataModel adm : atks)
			if (adm.range)
				return true;
		return false;
	}

	@Override
	public int[][] rawAtkData() {
		int[][] ans = new int[atks.length][];
		for (int i = 0; i < atks.length; i++)
			ans[i] = atks[i].getAtkData();
		return ans;
	}

	@Override
	public int touchBase() {
		return base == 0 ? range : base;
	}

	private boolean contains(String str) {
		if (atks == null || atks.length == 0)
			return false;
		for (AtkDataModel adm : atks)
			if (adm != null && adm.str.equals(str))
				return true;
		return false;
	}

	private void importData$1(CustomEntity ce) {
		base = ce.base;
		common = ce.common;
		kbBounce = ce.kbBounce;
		bossBounce = ce.bossBounce;

		rep = new AtkDataModel(this, ce.rep);
		rev = ce.rev != null ? new AtkDataModel(this, ce.rev) : null;
		res = ce.res != null ? new AtkDataModel(this, ce.res) : null;
		cntr = ce.cntr != null ? new AtkDataModel(this, ce.cntr) : null;
		bur = ce.bur != null ? new AtkDataModel(this, ce.bur) : null;
		resu = ce.resu != null ? new AtkDataModel(this, ce.resu) : null;
		revi = ce.revi != null ? new AtkDataModel(this, ce.revi) : null;
		glas = ce.glas != null ? new AtkDataModel(this, ce.glas) : null;

		List<AtkDataModel> temp = new ArrayList<>();
		List<AtkDataModel> tnew = new ArrayList<>();
		int[] inds = new int[ce.atks.length];
		for (int i = 0; i < ce.atks.length; i++) {
			if (!temp.contains(ce.atks[i])) {
				temp.add(ce.atks[i]);
				tnew.add(new AtkDataModel(this, ce.atks[i]));
			}
			inds[i] = temp.indexOf(ce.atks[i]);
		}
		atks = new AtkDataModel[ce.atks.length];
		for (int i = 0; i < atks.length; i++)
			atks[i] = tnew.get(inds[i]);
	}

	@JsonDecoder.OnInjected
	public void onInjected() {
		for (int i = 0; i < traits.size(); i++)
			if (traits.get(i) == null) {
				traits.remove(i);
				i--;
			}
	}

	@Override
	public Set<AbEnemy> getEnemySummon() {
		Set<AbEnemy> ans = new TreeSet<>();
		if (common) {
			if (rep.proc.SUMMON.prob > 0 && (rep.proc.SUMMON.id == null || AbEnemy.class.isAssignableFrom(rep.proc.SUMMON.id.cls)))
				ans.add(Identifier.getOr(rep.proc.SUMMON.id, AbEnemy.class));
		} else
			for (AtkDataModel adm : atks)
				if (adm.proc.SUMMON.prob > 0 && (adm.proc.SUMMON.id == null || AbEnemy.class.isAssignableFrom(adm.proc.SUMMON.id.cls)))
					ans.add(Identifier.getOr(adm.proc.SUMMON.id, AbEnemy.class));
		return ans;
	}

	@Override
	public Set<Unit> getUnitSummon() {
		Set<Unit> ans = new TreeSet<>();
		if (common) {
			if (rep.proc.SUMMON.prob > 0 && (rep.proc.SUMMON.id == null || Unit.class.isAssignableFrom(rep.proc.SUMMON.id.cls)))
				ans.add(Identifier.getOr(rep.proc.SUMMON.id, Unit.class));
		} else
			for (AtkDataModel adm : atks)
				if (adm.proc.SUMMON.prob > 0 && (adm.proc.SUMMON.id == null || Unit.class.isAssignableFrom(adm.proc.SUMMON.id.cls)))
					ans.add(Identifier.getOr(adm.proc.SUMMON.id, Unit.class));
		return ans;
	}
}
