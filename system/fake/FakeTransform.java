package common.system.fake;

/**
 * {@link FakeGraphics} の座標変換状態を描画基盤に依存しない形で保持する。
 * 取得元と同じ描画基盤の{@link FakeGraphics#setTransform(FakeTransform)}へ戻すことを前提とする。
 */
public interface FakeTransform {

	Object getAT();

}
