package common.battle;

import common.battle.data.PCoin;
import common.io.json.JsonClass;
import common.io.json.JsonDecoder.OnInjected;
import common.io.json.JsonField;
import common.pack.Identifier;
import common.pack.PackData;
import common.pack.UserProfile;
import common.util.Data;
import common.util.unit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * 2行5枠の形態配置と、ユニット単位のレベル・本能・本能玉設定を保持する。
 * {@link #renew()} は無効枠の除去後、戦闘用形態と成立コンボを再構築して各配列の整合を保つ。
 */
@SuppressWarnings("ForLoopReplaceableByForEach")
@JsonClass
public class LineUp extends Data {

	@JsonField(generic = { Identifier.class, Level.class })
	public final TreeMap<Identifier<Unit>, Level> map = new TreeMap<>();

	@JsonField(alias = Form.FormJson.class)
	public final Form[][] fs = new Form[2][5];
	public final EForm[][] efs = new EForm[2][5];
	public final EForm[][] spirits = new EForm[2][5];
	public int[] loc = new int[5];

	public List<Combo> coms = new ArrayList<>();

	private boolean updating = false;

	/**
	 * 空の編成を生成する。
	 */
	protected LineUp() {
		renew();
	}

	/**
	 * 形態配置とレベル設定を複製する。
	 */
	protected LineUp(LineUp ref) {
		for (int i = 0; i < 2; i++)
			System.arraycopy(ref.fs[i], 0, fs[i], 0, 5);

		for (Entry<Identifier<Unit>, Level> e : ref.map.entrySet()) {
			map.put(e.getKey(), e.getValue().clone());
		}

		renew();
	}

	/**
	 * 空き枠を詰め、全形態を小さい1次元添字側へ移動する。
	 */
	public void arrange() {
		for (int i = 0; i < 10; i++)
			if (getFS(i) == null)
				for (int j = i + 1; j < 10; j++)
					if (getFS(j) != null) {
						setFS(getFS(j), i);
						setFS(null, j);
						break;
					} else if (j == 9)
						return;
	}

	/**
	 * 指定コンボが現在の成立一覧にあるかを返す。
	 */
	public boolean contains(Combo c) {
		for (Combo com : coms)
			if (com == c)
				return true;
		return false;
	}

	/**
	 * 形態のレベル設定を返し、未登録なら既定値を登録する。
	 */
	public synchronized Level getLv(Form f) {
		if (!map.containsKey(f.unit.id))
			setLv(f.unit, f.unit.getPrefLvs());

		return validateLevel(f, map.get(f.unit.id));
	}

	/**
	 * 指定コンボを成立させるために1行目へ追加が必要な枠数を返す。
	 */
	public int occupance(Combo c) {
		Form[] com = c.forms;
		int rem = com.length;
		for (Form form : com)
			for (int j = 0; j < 5; j++) {
				Form f = fs[0][j];
				if (f == null)
					continue;
				if (f.unit == form.unit)
					rem--;
			}
		return rem;
	}

	@OnInjected
	public void renew() {
		validate();
		renewEForm();
		renewCombo();
	}

	/**
	 * 既存コンボを必要に応じて外し、指定コンボを成立させる形態を1行目へ配置する。
	 */
	public void set(Form[] com) {
		// 編成中の各枠が指定コンボに含まれるか
		boolean[] rep = new boolean[5];
		// 指定コンボの各ユニットが編成済みか
		boolean[] exi = new boolean[com.length];
		// 追加が必要なユニット数
		int rem = com.length;
		for (int i = 0; i < com.length; i++)
			for (int j = 0; j < 5; j++) {
				Form f = fs[0][j];
				int formID = com[i].fid;
				if (f == null)
					continue;
				if (f.unit == com[i].unit) {
					rep[j] = true;
					exi[i] = true;
					if (f.fid < formID)
						fs[0][j] = f.unit.forms[formID];
					loc[j]++;
					rem--;
				}
			}
		// 既存コンボに使われていない枠数
		int free = 0;
		for (int i = 0; i < 5; i++)
			if (loc[i] == 0)
				free++;

		if (free < rem) {
			// 空きが足りるまで既存コンボを外す

			int del = rem - free;
			while (del > 0) {
				Combo c = coms.remove(0);
				for (int i = 0; i < c.forms.length; i++) {
					if (c.forms[i] == null)
						break;
					for (int j = 0; j < 5; j++) {
						Form f = fs[0][j];
						if (f == null)
							break;
						if (f.unit != c.forms[i].unit)
							continue;
						loc[j]--;
						if (loc[j] == 0)
							del--;
						break;
					}
				}
			}
		}
		for (int i = 0; i < 5; i++)
			for (Form form : com)
				if (fs[1][i] != null && fs[1][i].unit == form.unit) {
					fs[1][i] = null;
					break;
				}
		arrange();
		int emp = 0;
		for (int i = 0; i < 10; i++)
			if (getFS(i) == null)
				emp++;
		if (emp < rem) {
			for (int i = 10 - rem; i < 10 - emp; i++)
				setFS(null, i);
			emp = rem;
		}
		int p = 0, r = 0, i = 0, j = 10 - emp;
		while (r < rem) {
			while (loc[i] != 0)
				i++;
			while (exi[p])
				p++;
			setFS(getFS(i), j++);
			Form c = com[p++];
			setFS(c, i++);
			r++;
		}
		renew();
	}

	/**
	 * ユニットのレベル設定を更新し、必要なら戦闘用形態を再構築する。
	 */
	public synchronized void setLv(Unit u, Level lv) {
		boolean sub = updating;
		updating = true;

		Level l = map.get(u.id);

		if (l != null) {
			l.setLvs(lv);
		} else {
			l = lv.clone();

			map.put(u.id, l);
		}

		if (!sub)
			renewEForm();

		updating &= sub;
	}

	/**
	 * ユニットの本能玉設定を更新し、必要なら戦闘用形態を再構築する。
	 */
	public synchronized void setOrb(Unit u, Level lv, int[][] orbs) {
		// 本能玉設定より先にレベル設定を用意する
		boolean sub = updating;
		updating = true;

		Level l = map.get(u.id);

		if (l != null) {
			l.setLvs(lv);
			l.setOrbs(orbs);
		} else {
			l = lv.clone();
			l.setOrbs(orbs);
			map.put(u.id, l);
		}

		if (!sub)
			renewEForm();

		updating &= sub;
	}

	/**
	 * 指定コンボの配置に既存コンボの解除が必要かを返す。
	 */
	public boolean willRem(Combo c) {
		int free = 0;

		for (int i = 0; i < 5; i++)
			if (fs[0][i] == null)
				free++;
			else if (loc[i] == 0) {
				boolean b = true;

				for (Form is : c.forms)
					if (fs[0][i].unit == is.unit) {
						b = false;

						break;
					}
				if (b)
					free++;
			}

		return free < occupance(c);
	}

	/**
	 * 0から9の添字を2行5枠へ変換して設定する。
	 */
	protected void setFS(Form f, int i) {
		fs[i / 5][i % 5] = f;
	}

	/**
	 * 0から9の添字を2行5枠へ変換して取得する。
	 */
	private Form getFS(int i) {
		return fs[i / 5][i % 5];
	}

	/**
	 * 1行目から成立コンボと各枠の使用数を再計算する。
	 */
	private void renewCombo() {
		List<Combo> tcom = new ArrayList<>();
		loc = new int[5];
		for (PackData p : UserProfile.getAllPacks()) {
			if (p instanceof PackData.UserPack && !((PackData.UserPack)p).useCombos)
				continue;

			for (Combo c : p.combos) {
				boolean b = true;
				for (int i = 0; i < c.forms.length; i++) {
					Form fu = c.forms[i];
					if (fu == null)
						break;
					boolean b0 = false;
					for (int j = 0; j < 5; j++) {
						Form f = fs[0][j];
						if (f == null)
							break;
						if (f.unit != fu.unit || f.fid < fu.fid)
							continue;
						b0 = true;
						break;
					}
					if (b0)
						continue;
					b = false;
					break;
				}
				if (b) {
					tcom.add(c);
					for (int i = 0; i < c.forms.length; i++)
						for (int j = 0; j < 5; j++) {
							Form fu = c.forms[i];
							Form f = fs[0][j];
							if (f == null || fu == null)
								continue;
                            if (f.unit == fu.unit && f.fid >= fu.fid)
								loc[j]++;
						}
				}
			}
		}
		for (int i = 0; i < coms.size(); i++)
			if (!tcom.contains(coms.get(i))) {
				coms.remove(i);
				i--;
			}

		for (int i = 0; i < tcom.size(); i++)
			if (!coms.contains(tcom.get(i)))
				coms.add(tcom.get(i));
	}

	private void renewEForm() {
		for (int i = 0; i < 2; i++)
			for (int j = 0; j < 5; j++) {
				Form form = fs[i][j];
				if (form == null) {
					efs[i][j] = null;
					spirits[i][j] = null;
				} else {
					efs[i][j] = new EForm(form, getLv(form));
					efs[i][j].getLevel().revalidateOrb(form.unit);

					if (fs[i][j].du.getProc().SPIRIT.exists()) {
						Unit u = Identifier.getOr(form.du.getProc().SPIRIT.id, Unit.class);
						Form spiritForm = u.forms[0];

						Level spiritLevel = getLv(form).clone();
						spiritLevel.setLevel(Math.min(u.max, spiritLevel.getLv() + spiritLevel.getPlusLv()));
						spiritLevel.setPlusLevel(0);
						spiritLevel.setOrbs(null);

						Arrays.fill(spiritLevel.getTalents(), 0);
						spirits[i][j] = new EForm(spiritForm, spiritLevel);
					}
				}
			}
	}

	private void validate() {
		for (int i = 0; i < 10; i++)
			if (getFS(i) != null) {
				Identifier<Unit> id = getFS(i).uid;
				int f = getFS(i).fid;
				Unit u = Identifier.get(id);
				if (u == null || f >= u.forms.length || u.forms[f] == null)
					setFS(null, i);
			}
		arrange();
	}

	private Level validateLevel(Form f, Level lv) {
		Unit u = f.unit;

		int maxTalent = 0;
		PCoin pc = null;

		for(Form form : u.forms) {
			if(form.du.getPCoin() != null && form.du.getPCoin().max.length > maxTalent) {
				pc = form.du.getPCoin();
				maxTalent = pc.max.length;
			}
		}

		lv.setLevel(Math.max(1, Math.min(u.max, lv.getLv())));
		lv.setPlusLevel(Math.max(0, Math.min(u.maxp, lv.getPlusLv())));

		if(pc != null) {
			int[] max = pc.max;

			if(lv.getTalents().length < max.length) {
				int[] talents = new int[max.length];

				for(int i = 0; i < lv.getTalents().length; i++) {
					talents[i] = lv.getTalents()[i];
				}

				if (max.length - lv.getTalents().length >= 0)
					System.arraycopy(max, lv.getTalents().length, talents, lv.getTalents().length, max.length - lv.getTalents().length);

				lv.setTalents(talents);
			}
		}

		return lv;
	}
}
