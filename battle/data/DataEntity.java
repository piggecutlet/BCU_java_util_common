package common.battle.data;

import common.io.json.JsonClass;
import common.io.json.JsonClass.NoTag;
import common.io.json.JsonField;
import common.pack.Identifier;
import common.util.Data;
import common.util.pack.Soul;
import common.util.unit.Trait;

import java.util.ArrayList;
import java.util.List;

/**
 * 味方・敵に共通する耐久、移動、射程、属性などの静的戦闘値を保持する基底データ。
 * 旧形式の属性ビット列とシールド値は読み込み互換のためだけに残されている。
 */
@JsonClass(noTag = NoTag.LOAD)
public abstract class DataEntity extends Data implements MaskEntity {

	public int hp, hb, speed, range;
	public int abi, width;
	public int loop = -1, will;
	@JsonField(io = JsonField.IOType.R)
	public int type, shield;

	public Identifier<Soul> death;
	@JsonField(generic = Trait.class, alias = Identifier.class)
	public ArrayList<Trait> traits = new ArrayList<>();
	// 属性構造移行前のデータを新しい属性一覧へ移すため、type を残している
	// type と shield は 0-5-1-1 で安全に削除できる見込み

	@Override
	public int getAbi() {
		return abi;
	}

	@Override
	public int getAtkLoop() {
		return loop;
	}

	@Override
	public Identifier<Soul> getDeathAnim() {
		return death;
	}

	@Override
	public List<Trait> getTraits() {
		return traits;
	}

    @Override
    public List<Trait> getTraitsRaw() {
        return traits;
    }

    @Override
	public int getHb() {
		return hb;
	}

	@Override
	public int getHp() {
		return hp;
	}

	@Override
	public int getRange() {
		return range;
	}

	@Override
	public int getSpeed() {
		return speed;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getWill() {
		return will;
	}

	@JsonField(tag = "type", io = JsonField.IOType.R)
	public void genType(int t) {
		type = t;
	}

	@JsonField(tag = "shield", io = JsonField.IOType.R)
	public void genShield(int s) {
		shield = s;
	}

}
