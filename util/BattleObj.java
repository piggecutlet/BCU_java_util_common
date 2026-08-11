package common.util;

import common.CommonStatic;
import common.battle.BattleField;
import common.io.BCUException;
import common.io.assets.Admin.StaticPermitted;
import common.pack.Context.ErrType;
import common.util.pack.bgeffect.BackgroundEffect;
import common.util.unit.EneRand;
import common.util.unit.Trait;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 相互参照を含む戦闘オブジェクト群を一括複製するための基底型。<br>
 * <br>
 * プリミティブ、文字列、対象の戦闘オブジェクト、配列、および引数なしコンストラクタを持つ
 * Collection・Map を再帰的に複製する。共有資源として {@link #EXCLUDE} に列挙された型は参照を共有する。<br>
 * 同一インスタンスの対応付けは複製処理中だけ保持され、{@link #clone()} 完了時に破棄される。
 */
@StaticPermitted(StaticPermitted.Type.TEMP)
public class BattleObj extends ImgCore implements Cloneable {

	public static final String NONC = "NONC_";

	private static final Class<?>[] EXCLUDE = { Number.class, String.class, Boolean.class, BattleStatic.class, Trait.class, Enum.class, BackgroundEffect.class, BattleField.class, EneRand.class };

	private static final Set<Class<?>> OLD = new HashSet<>();
	private static final Set<Class<?>> UNCHECKED = new HashSet<>();
	private static final Map<Integer, Object> ARRMAP = new HashMap<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected static Object hardCopy(Object obj) {
		if (obj == null)
			return null;
		Class<?> c = obj.getClass();
		if (c.isPrimitive())
			return obj;
		for (Class<?> cls : EXCLUDE)
			if (cls.isAssignableFrom(c))
				return obj;
		if (obj instanceof BattleObj)
			return ((BattleObj) obj).sysCopy();
		if (ARRMAP.containsKey(obj.hashCode()))
			return ARRMAP.get(obj.hashCode());
		if (obj.getClass().isArray()) {
			Object ans = Array.newInstance(c.getComponentType(), Array.getLength(obj));
			for (int i = 0; i < Array.getLength(ans); i++)
				Array.set(ans, i, hardCopy(Array.get(obj, i)));
			ARRMAP.put(obj.hashCode(), ans);
			return ans;
		}
		if (Collection.class.isAssignableFrom(c)) {
			Collection f2 = (Collection) obj;
			Collection f3 = null;
			try {
				f3 = f2.getClass().getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (f3 != null)
				for (Object o : f2)
					f3.add(hardCopy(o));
			return f3;
		}
		if (Map.class.isAssignableFrom(c)) {
			Map f2 = (Map) obj;
			Map f3 = null;
			try {
				f3 = f2.getClass().getConstructor().newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Map f4 = f3;
			if (f4 != null)
				f2.forEach((k, v) -> f4.put(hardCopy(k), hardCopy(v)));
			return f3;
		}
		throw new BCUException("cannot copy class " + obj.getClass());
	}

	private static boolean checkField(Class<?> tc) {
		if (tc.isPrimitive())
			return true;
		boolean b0 = BattleObj.class.isAssignableFrom(tc);
		boolean b1 = BattleStatic.class.isAssignableFrom(tc);
		if (b0 && b1)
			return false;
		if (b0 || b1)
			return true;
		for (Class<?> cls : EXCLUDE)
			if (cls.isAssignableFrom(tc))
				return true;
		if (tc.isArray())
			return checkField(tc.getComponentType());
		return false;
	}

	@SuppressWarnings("unchecked")
	private static List<Field> getField(Class<? extends BattleObj> cls) {
		List<Field> fl = new ArrayList<Field>();
		Field[] fs = cls.getDeclaredFields();
		for (Field f : fs)
			if (!Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				fl.add(f);
			}
		Class<? extends BattleObj> sc = null;
		if (BattleObj.class.isAssignableFrom(cls) && BattleObj.class != cls.getSuperclass())
			sc = (Class<? extends BattleObj>) cls.getSuperclass();
		if (sc != null)
			fl.addAll(getField(sc));
		return fl;
	}

	protected BattleObj copy = null;

	@Override
	public final BattleObj clone() {
		BattleObj c = sysCopy();
		terminate();
		ARRMAP.clear();
		UNCHECKED.removeAll(OLD);
		for (Class<?> cls : UNCHECKED)
			CommonStatic.ctx.printErr(ErrType.WARN, "Unchecked Class in Battle: " + cls);
		OLD.addAll(UNCHECKED);
		UNCHECKED.clear();
		return c;
	}

	/**
	 * BattleStatic と同名だが戻り値が異なり、両方を実装する型をコンパイル時に排除する。
	 */
	public final int conflict() {
		return 0;
	}

	/**
	 * 全フィールドを既定どおり複製しない場合に、独自の複製処理を実装する。<br>
	 * <br>
	 * {@link #copy} が浅い複製で初期化された後に呼ばれる。
	 */
	protected void performDeepCopy() {
		List<Field> lf = getField(getClass());
		check(lf);
		for (Field f : lf) {
			if (f.getName().startsWith(NONC))
				continue;
			try {
				f.setAccessible(true);
				f.set(copy, hardCopy(f.get(this)));
			} catch (Exception e3) {
				System.out.println("failed to copy class " + getClass() + " at field " + f);
				e3.printStackTrace();
			}
		}
	}

	/**
	 * 複製処理で使用した一時的な相互参照を再帰的に解放する。<br>
	 * <br>
	 * {@link #clone()} の最後に元オブジェクト側から呼ばれる。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void terminate() {
		if (copy == null)
			return;
		BattleObj temp = copy;
		copy = null;
		if (temp != null)
			temp.terminate();
		List<Field> lf = getField(getClass());
		for (Field f : lf) {
			f.setAccessible(true);
			Class<?> tc = f.getType();

			if (BattleObj.class.isAssignableFrom(tc)) {
				BattleObj f2 = null;
				try {
					f2 = (BattleObj) f.get(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (f2 != null)
					f2.terminate();
			}
			if (tc.isArray() && BattleObj.class.isAssignableFrom(tc.getComponentType())) {
				BattleObj[] f2 = null;
				try {
					f2 = (BattleObj[]) f.get(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (f2 != null)
					for (BattleObj c : f2)
						if (c != null)
							c.terminate();
			}
			if (Collection.class.isAssignableFrom(tc)) {
				if (f.getName().equals(NONC))
					continue;
				Collection f2 = null;
				try {
					f2 = (Collection) f.get(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (f2 != null)
					for (Object c : f2)
						if (c != null && c instanceof BattleObj)
							((BattleObj) c).terminate();
			}
			if (Map.class.isAssignableFrom(tc)) {
				if (f.getName().equals(NONC))
					continue;
				Map f2 = null;
				try {
					f2 = (Map) f.get(this);
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (f2 != null)
					f2.forEach((a, b) -> {
						if (a instanceof BattleObj)
							((BattleObj) a).terminate();
						if (b instanceof BattleObj)
							((BattleObj) b).terminate();
					});
			}
		}
	}

	/**
	 * 既定の複製規則で扱えない実行時型を検出する。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void check(List<Field> lf) {
		for (Field f : lf) {
			Object obj = null;
			try {
				obj = f.get(this);
			} catch (IllegalAccessException e1) {
				e1.printStackTrace();
			}
			if (obj == null)
				continue;
			Class<?> tc = obj.getClass();
			if (checkField(tc))
				continue;
			if (Collection.class.isAssignableFrom(tc)) {
				Collection f2 = (Collection) obj;
				for (Object o : f2)
					if (!checkField(o.getClass()))
						UNCHECKED.add(o.getClass());
				continue;
			}
			if (Map.class.isAssignableFrom(tc)) {
				Map f2 = (Map) obj;
				f2.forEach((a, b) -> {
					if (!checkField(a.getClass()))
						UNCHECKED.add(a.getClass());
					if (!checkField(b.getClass()))
						UNCHECKED.add(b.getClass());
				});
				continue;
			}
			UNCHECKED.add(tc);
		}
	}

	/**
	 * 一括複製中にこのオブジェクトの対応先を取得する。
	 * すでに複製済みなら同じ対応先を返し、循環参照と共有参照を維持する。
	 */
	private BattleObj sysCopy() {
		if (copy != null)
			return copy;
		try {
			// まずプリミティブ値と参照を浅く複製する
			copy = (BattleObj) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		copy.copy = this;
		performDeepCopy();
		return copy;
	}

}
