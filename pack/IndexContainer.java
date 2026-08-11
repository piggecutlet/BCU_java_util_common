package common.pack;

import common.pack.FixIndexList.FixIndexMap;

import java.lang.annotation.*;

/**
 * パックIDと型別の疎な固定インデックス表を結び付け、{@link Identifier}の生成・解決を共通化する。
 * 表示順と固定IDは分離され、参照は常に固定ID側で解決される。
 */
public interface IndexContainer {

	/**
	 * 新しい固定IDを受け取り、対応する要素を生成する契約。
	 */
	public static interface Constructor<T extends R, R extends Indexable<?, R>> {

		T get(Identifier<R> id);

	}

	/**
	 * パックIDからコンテナを返す静的メソッドを示す標識。
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface ContGetter {
	}

	/**
	 * 自身の固定IDを介して所有コンテナへ到達できる要素の契約。
	 */
	interface Indexable<R extends IndexContainer, T extends Indexable<R, T>> {

		@SuppressWarnings("unchecked")
		default R getCont() {
			return (R) getID().getCont();
		}

		Identifier<T> getID();

	}

	/**
	 * 要素型またはその上位型を、所有コンテナ型へ関連付ける標識。
	 */
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface IndexCont {

		Class<? extends IndexContainer> value();

	}

	/**
	 * 型に対応する固定インデックス表へ畳み込み処理を適用する契約。
	 */
	interface Reductor<R, T> {

		R reduce(R r, T t);

	}

	/**
	 * 1種類の固定インデックス表だけを所有するコンテナ向けの簡易契約。
	 */
	public static interface SingleIC<T extends Indexable<?, T>> extends IndexContainer {

		default T add(Constructor<T, T> con) {
			int ind = getFIM().nextInd();
			T ans = con.get(getID(ind));
			getFIM().set(ind, ans);
			return ans;
		}

		default T add(int ind, Constructor<T, T> con) {
			T ans = con.get(getID(ind));
			getFIM().set(ind, ans);
			return ans;
		}

		FixIndexMap<T> getFIM();

		@SuppressWarnings("unchecked")
		@Override
		@Deprecated
		default <X extends R, R extends Indexable<?, R>> Identifier<R> getID(Class<X> cls, int ind) {
			return (Identifier<R>) getID(ind);
		}

		default Identifier<T> getID(int ind) {
			return new Identifier<T>(getSID(), getFIM().cls, ind);
		}

		@Override
		@Deprecated
		@SuppressWarnings("rawtypes")
		default <R> R getList(Class cls, Reductor<R, FixIndexMap> func, R def) {
			return func.reduce(def, getFIM());
		}

		default Identifier<T> getNextID() {
			return new Identifier<T>(getSID(), getFIM().cls, getFIM().nextInd());
		}

		@SuppressWarnings("unchecked")
		@Override
		@Deprecated
		default <X extends R, R extends Indexable<?, R>> Identifier<R> getNextID(Class<X> cls) {
			return (Identifier<R>) getNextID();
		}

	}

	default <T extends R, R extends Indexable<?, R>> T add(FixIndexMap<R> map, Constructor<T, R> con) {
		int ind = map.nextInd();
		T ans = con.get(getID(map.cls, ind));
		map.set(ind, ans);
		return ans;
	}

	default <T extends R, R extends Indexable<?, R>> T add(FixIndexMap<R> map, int ind, Constructor<T, R> con) {
		T ans = con.get(getID(map.cls, ind));
		map.set(ind, ans);
		return ans;
	}

	default <T extends R, R extends Indexable<?, R>> Identifier<R> getID(Class<T> cls, int ind) {
		return new Identifier<R>(getSID(), cls, ind);
	}

	@SuppressWarnings("rawtypes")
	<R> R getList(Class cls, Reductor<R, FixIndexMap> func, R def);

	default <T extends R, R extends Indexable<?, R>> Identifier<R> getNextID(Class<T> cls) {
		int id = getList(cls, (r, l) -> l.nextInd(), 0);
		return new Identifier<R>(getSID(), cls, id);
	}

	String getSID();

}