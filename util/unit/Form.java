package common.util.unit;

import common.CommonStatic;
import common.battle.data.*;
import common.io.json.JsonClass;
import common.io.json.JsonClass.*;
import common.io.json.JsonDecoder.OnInjected;
import common.io.json.JsonField;
import common.pack.Identifier;
import common.pack.PackData;
import common.pack.UserProfile;
import common.system.BasedCopable;
import common.util.Animable;
import common.util.Data;
import common.util.anim.AnimU;
import common.util.anim.AnimUD;
import common.util.anim.EAnimU;
import common.util.anim.MaModel;
import common.util.lang.MultiLangCont;
import common.util.lang.MultiLangData;

import javax.annotation.Nullable;

/**
 * 1キャラの特定形態について、能力・特性・攻撃・アニメーションを保持する定義。
 * キャラ識別子{@link #uid}と形態番号{@link #fid}の組で参照し、現在レベルや戦闘中状態は保持しない。
 */
@JCGeneric(Form.FormJson.class)
@JsonClass(read = RType.FILL)
public class Form extends Animable<AnimU<?>, AnimU.UType> implements BasedCopable<Form, Unit> {

	/**
	 * フォーム参照をキャラ識別子と形態番号だけで直列化する代理値。
	 */
	@JsonClass(noTag = NoTag.LOAD)
	public static class FormJson {

		public Identifier<Unit> uid;
		public int fid;

		@JCConstructor
		public FormJson() {
		}

		@JCConstructor
		public FormJson(Form f) {
			uid = f.uid;
			fid = f.fid;
		}

		@JCGetter
		public Form get() {
			try {
				return uid.get().forms[fid];
			} catch (Exception e) {
				return null;
			}
		}
	}

	public static String lvString(int[] lvs) {
		StringBuilder str = new StringBuilder("Lv." + lvs[0] + ", {");
		for (int i = 1; i < 5; i++)
			str.append(lvs[i]).append(",");
		str.append(lvs[5]).append("}");
		return str.toString();
	}

	@JsonField
	public final MaskUnit du;
	public final Unit unit;
	public final Identifier<Unit> uid;
	@JsonField
	public int fid;

	@JsonField(io = JsonField.IOType.R)
	public String name = "";
	@JsonField(generic = MultiLangData.class)
	public MultiLangData names = new MultiLangData();

	@JsonField(io = JsonField.IOType.R)
	public String explanation = "<br><br><br>";
	@JsonField(generic = MultiLangData.class)
	public MultiLangData description = new MultiLangData("<br><br><br>");

	@JCConstructor
	public Form(Unit u) {
		du = null;
		unit = u;
		uid = unit.id;
	}

	public Form(Unit u, int f, String str, AnimU<?> ac, CustomUnit cu) {
		unit = u;
		uid = u.id;
		fid = f;
		names.put(str);
		anim = ac;
		du = cu;
		cu.pack = this;
	}

	// 公式の通常キャラ用
	protected Form(Unit u, int f, String str, String data) {
		unit = u;
		uid = u.id;
		fid = f;
		String nam = trio(uid.id) + "_" + SUFX[fid];
		anim = new AnimUD(str, nam, "edi" + nam + ".png", "uni" + nam + "00.png");
		anim.getUni().setCut(CommonStatic.getBCAssets().unicut);
		String[] strs = data.split("//")[0].trim().split(",");
		du = new DataUnit(this, strs);
		MaModel model = anim.loader.getMM();
		((DataUnit) du).limit = CommonStatic.dataFormMinPos(model);
	}
	// 公式のタマゴキャラ用
	protected Form(Unit u, int f, int m, String str, String data) {
		unit = u;
		uid = u.id;
		fid = f;
		String nam = trio(m) + "_m";
		anim = new AnimUD(str, nam, "edi" + nam + duo(fid) + ".png", "uni" + nam + duo(fid) + ".png");
		anim.getUni().setCut(CommonStatic.getBCAssets().unicut);
		String[] strs = data.split("//")[0].trim().split(",");
		du = new DataUnit(this, strs);
		MaModel model = anim.loader.getMM();
		((DataUnit) du).limit = CommonStatic.dataFormMinPos(model);
	}

	@Override
	public Form copy(Unit b) {
		CustomUnit cu = new CustomUnit();
		cu.importData(du);
		return new Form(b, fid, names.toString(), anim, cu);
	}

	public int getDefaultPrice(int sta) {
		MaskUnit upc = maxu();
		return (int) (upc.getPrice() * (1 + sta * 0.5));
	}

	@Override
	public EAnimU getEAnim(AnimU.UType t) {
		return anim.getEAnim(t);
	}

	public MaskUnit maxu() {
		PCoin pc = du.getPCoin();
		if (pc != null) {
			return pc.full;
		}
		return du;
	}

	@OnInjected
	public void onInjected() {
		CustomUnit data = (CustomUnit) du;
		data.pack = this;

		if((unit != null || uid != null)) {
			Unit u = unit == null ? uid.get() : unit;

			if(u.getCont() instanceof PackData.UserPack) {
				PackData.UserPack pack = (PackData.UserPack) u.getCont();
				Proc proc = data.getProc();

				if (UserProfile.isOlderPack(pack, "0.5.1.0")) {
					data.type = Data.reorderTrait(data.type);
				}

				if (UserProfile.isOlderPack(pack, "0.5.2.0") && data.tba != 0) {
					data.tba += data.getPost() + 1;
				}

				if (UserProfile.isOlderPack(pack, "0.6.0.0")) {
					MaModel model = anim.loader.getMM();
					data.limit = CommonStatic.customFormMinPos(model);
					proc.BARRIER.health = data.shield;
					data.traits = Trait.bitmaskToTrait(data.type);
					if ((data.abi & (1 << 18)) != 0) // 封印無効
						proc.IMUSEAL.mult = 100;
					if ((data.abi & (1 << 7)) != 0) // 移動攻撃無効
						proc.IMUMOVING.mult = 100;
					if ((data.abi & (1 << 12)) != 0) // 毒撃無効
						proc.IMUPOI.mult = 100;
					data.abi = Data.reorderAbi(data.abi, 0);
				}

				if (UserProfile.isOlderPack(pack, "0.6.1.0")) {
					proc.DMGCUT.reduction = 100;
					proc.POISON.type.ignoreMetal = true;
				}

				if (UserProfile.isOlderPack(pack, "0.6.4.0")) {
					names.put(name);
					description.put(explanation);
				}

				if (UserProfile.isOlderPack(pack, "0.6.5.0")) {
					if ((data.abi & 16) > 0) // 撃破時のお金2倍
						proc.BOUNTY.mult = 100;
					if ((data.abi & 32) > 0) // 城破壊
						proc.ATKBASE.mult = 300;
					data.abi = Data.reorderAbi(data.abi, 1);
				}
				if (UserProfile.isOlderPack(pack, "0.6.6.0")) {
					if (proc.TIME.prob > 0)
						proc.TIME.intensity = proc.TIME.time;

					if (proc.SUMMON.prob > 0) {
						proc.SUMMON.max_dis = proc.SUMMON.dis;
						proc.SUMMON.min_layer = -1;
						proc.SUMMON.max_layer = -1;
					}
				}
				if (UserProfile.isOlderPack(pack, "0.7.4.1") && data.pcoin != null) {
					data.pcoin.info.forEach(i -> i[12] = -1);
				}
				if (UserProfile.isOlderPack(pack, "0.7.12.2") && data.pcoin != null) {
					data.pcoin.info.removeIf(d -> d[0] == 68);
					data.pcoin.update();
				}

				if (proc.SUMMON.prob > 0 && proc.SUMMON.form <= 0) {
					proc.SUMMON.form = 1;
					proc.SUMMON.mult = 1;
					proc.SUMMON.type.fix_buff = true;
				}

				if ((UserProfile.isOlderPack(pack, "0.7.19.1") || proc.SUMMON.type.layer_type == null) && proc.SUMMON.prob > 0) {
					if (proc.SUMMON.min_layer == proc.SUMMON.max_layer && proc.SUMMON.min_layer == -1)
						proc.SUMMON.type.layer_type = CommonStatic.LayerType.ORIG;
					else
						proc.SUMMON.type.layer_type = CommonStatic.LayerType.SET;
				}

				for (AtkDataModel adm : data.atks)
					adm.inject(pack);
				data.rep.inject(pack);
				for (int i = 0; i < data.getAtkCount() + CustomEntity.SPECIAL_ATTACK_COUNT; i++) {
					AtkDataModel model = (AtkDataModel) data.getAtkModel(data.getAtkCount() + i);
					if (model != null)
						model.inject(pack);
				}
			}
		}
		if (data.getPCoin() != null) {
			data.pcoin.verify();
			data.pcoin.update();
		}
	}

	/**
	 * 基本・プラス・本能レベルをこのフォームとユニットの上限内へ収める。
	 * @param src {@code target}へ反映するレベル。{@code null}なら{@code target}自身を検証する
	 * @param target 検証および更新対象のレベル
	 * @return 更新後の{@code target}
	 */
	public Level regulateLv(@Nullable Level src, Level target) {
		if(src != null) {
			target.setLevel(Math.max(1, Math.min(src.getLv(), unit.max)));
			target.setPlusLevel(Math.max(0, Math.min(src.getPlusLv(), unit.maxp)));

			PCoin pc = du.getPCoin();

			if (pc != null) {
				int[] maxTalents = new int[pc.info.size()];

				for (int i = 0; i < pc.info.size(); i++)
					maxTalents[i] = Math.max(1, pc.info.get(i)[1]);

				int[] t = new int[maxTalents.length];

				for (int i = 0; i < Math.min(maxTalents.length, src.getTalents().length); i++) {
					t[i] = Math.min(maxTalents[i], Math.max(0, src.getTalents()[i]));
				}

				if (src.getTalents().length < target.getTalents().length) {
					for (int i = src.getTalents().length; i < Math.min(maxTalents.length, target.getTalents().length); i++) {
						t[i] = Math.min(maxTalents[i], Math.min(0, target.getTalents()[i]));
					}
				}

				target.setTalents(t);
			}
		} else {
			target.setLevel(Math.max(1, Math.min(unit.max, target.getLv())));
			target.setPlusLevel(Math.max(0, Math.min(unit.maxp, target.getPlusLv())));

			PCoin pc = du.getPCoin();

			if (pc != null) {
				int[] maxTalents = new int[pc.info.size()];
				int[] t = new int[pc.info.size()];

				for (int i = 0; i < pc.info.size(); i++)
					maxTalents[i] = Math.max(1, pc.info.get(i)[1]);

				for (int i = 0; i < Math.min(maxTalents.length, target.getTalents().length); i++) {
					t[i] = Math.min(maxTalents[i], Math.max(0, target.getTalents()[i]));
				}

				target.setTalents(t);
			}
		}

		return target;
	}

	@Override
	public String toString() {
		String base = (uid == null ? "NULL" : uid.id) + "-" + fid + " ";
		String desp = MultiLangCont.get(this);
		if (desp != null && !desp.isEmpty())
			return base + desp;

		String nam = names.toString();
		if (!nam.isEmpty())
			return base + nam;
		return base;
	}

	public String getExplaination() {
		String[] desp = MultiLangCont.getDesc(this);
		if (desp != null && !desp[fid + 1].isEmpty())
			return desp[fid + 1];
		return description.toString();
	}

	public boolean hasEvolveCost() {
		return unit.info.hasEvolveCost() && fid == 2;
	}

	public boolean hasZeroForm() {
		return unit.info.hasZeroForm() && fid == 3;
	}
}