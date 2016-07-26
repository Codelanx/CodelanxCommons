package com.codelanx.commons.util.stream;

import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator.OfInt;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A debuggable and extendable {@link IntStream} wrapper, which allows overriding of only
 * two methods to allow seeing stream operation in realtime.
 *
 * @since 0.3.1
 * @version 0.3.1
 */
public class DebugIntStream implements IntStream, DebuggableStreaming<Integer> {

    private static final Map<Class<? extends DebugIntStream>, Constructor<? extends DebugIntStream>> CONSTRUCTORS = new HashMap<>();
    private static final Constructor<? extends DebugIntStream> DEFAULT;

    static {
        Constructor<? extends DebugIntStream> set = null;
        try {
            set = DebugIntStream.class.getDeclaredConstructor(IntStream.class, boolean.class);
            set.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        DEFAULT = set;
    }

    private final IntStream stream;
    private final boolean outputResults;
    private final Map<String, Integer> methodCount = new HashMap<>();

    protected DebugIntStream(IntStream stream, boolean outputResults) {
        this.stream = stream;
        this.outputResults = outputResults;
    }

    /**
     * Creates a new {@link DebugIntStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link IntStream} to wrap
     * @return A new {@link DebugIntStream} wrapper of the provided {@link IntStream}
     */
    public static DebugIntStream of(IntStream stream) {
        return DebugIntStream.of(stream, false);
    }

    /**
     * Creates a new {@link DebugIntStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link IntStream} to wrap
     * @param outputResults {@code true} to output the results of the {@link IntStream} as it is operated upon
     * @return A new {@link DebugIntStream} wrapper of the provided {@link IntStream}
     */
    public static DebugIntStream of(IntStream stream, boolean outputResults) {
        return DebugIntStream.of(stream, outputResults, null);
    }

    /**
     * Instantiates a new {@link DebugIntStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugIntStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link IntStream} to create a {@link DebugIntStream} of.
     * @param type The class representing the type of the {@link DebugIntStream} to instantiate
     * @return A new {@link DebugIntStream} of the provided {@link IntStream}
     */
    public static DebugIntStream of(IntStream stream, Class<? extends DebugIntStream> type) {
        return DebugIntStream.of(stream, false, type);
    }

    /**
     * Instantiates a new {@link DebugIntStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugIntStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link IntStream} to create a {@link DebugIntStream} of.
     * @param type The class representing the type of the {@link DebugIntStream} to instantiate
     * @return A new {@link DebugIntStream} of the provided {@link IntStream}, or the base class instance of
     *         {@link DebugIntStream} if the type either failed to instantiate or was {@code null}.
     */
    public static DebugIntStream of(IntStream stream, boolean outputResults, Class<? extends DebugIntStream> type) {
        if (type != null && type != DebugIntStream.class) {
            try {
                Constructor<? extends DebugIntStream> cont = CONSTRUCTORS.<Constructor<? extends DebugIntStream>>computeIfAbsent(type, k -> {
                    try {
                        Constructor<? extends DebugIntStream> c = k.getDeclaredConstructor(IntStream.class, boolean.class);
                        c.setAccessible(true);
                        return c;
                    } catch (NoSuchMethodException e) {
                        Debugger.error(e, "No constructor which accepts a (Stream, boolean) for class %s, developer error?", type.getName());
                        e.printStackTrace();
                    }
                    return DEFAULT;
                });
                cont.setAccessible(true);
                return cont.newInstance(stream);
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException ex) {
                Debugger.error(ex, "Could not instantiate DebugIntStream of type %s, returning default...", type.getName());
                ex.printStackTrace();
            }
        }
        return new DebugIntStream(stream, outputResults);
    }

    /**
     * Overridable method which runs the appropriate {@link IntStream} action, and outputs the result as well as
     * which numerical call it is. For example, the third #filter calll will be printed as
     * {@code DebugStream#filter[2]: &lt;result&gt;}. The output of {@code result} will be substringed to a maximum
     * of 50 characters, to keep the console from being spammed with large datasets. Additionally, method names with
     * differing method parameters will be grouped under the same aliases and therefore, the same method "counter".
     * <br /><br />
     * Please note the performance impacts of this method, if this is a standard stream operation and result output
     * is enabled, the {@link IntStream} will need to be recreated each time.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param action The {@link Function} representing the action taken on the {@link IntStream}
     * @param <R> The return type of the action
     * @return The result of the call on the {@link IntStream}
     */
    protected <R> R operate(Function<? super IntStream, ? extends R> action) {
        StackTraceElement elm = Reflections.getCaller();
        Integer amount = this.methodCount.compute(elm.getMethodName(), (k, old) -> old == null ? 0 : old + 1);
        R result = action.apply(this.stream);
        String output = null;
        if (this.outputResults) {
            if (result instanceof IntStream) {
                List<Integer> objs = ((IntStream) result).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                output = objs.toString();
                result = (R) objs.stream().mapToInt(Integer::valueOf);
            } else {
                output = Objects.toString(result);
            }
            output = output.substring(0, Math.min(50, output.length()));
        }
        System.out.println("DebugIntStream#" + elm.getMethodName() + "[" + amount + "]" + (output == null ? "" : ": " + output));
        return result;
    }

    /**
     * Overridable method which runs the appropriate {@link IntStream} action, and outputs the result as well as
     * which numerical call it is. Due to this being a void operation on a {@link Consumer}, the result output will
     * always be {@code null}.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @see DebugStream#operate(Function)
     * @param action The {@link Consumer} representing the action taken on the {@link IntStream}
     */
    protected void operateVoid(Consumer<? super IntStream> action) {
        this.operate(s -> {
            action.accept(s);
            return null;
        });
    }

    public static <D> DebugIntStream mapFromObject(DebuggableStreaming<D> debugStream, ToIntFunction<? super D> mapper) {
        return DebugIntStream.of(debugStream.getRawStream().mapToInt(mapper), debugStream.isOutputtingResults());
    }

    public static <D> DebugIntStream flatMapFromObject(DebuggableStreaming<D> debugStream, Function<? super D, ? extends IntStream> mapper) {
        return DebugIntStream.of(debugStream.getRawStream().flatMapToInt(mapper), debugStream.isOutputtingResults());
    }

    @Override
    public IntStream filter(IntPredicate predicate) {
        return this.operate(s -> s.filter(predicate));
    }

    @Override
    public IntStream map(IntUnaryOperator mapper) {
        return this.operate(s -> s.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
        return DebugStream.of(this.getRawStream().map(mapper::apply), this.isOutputtingResults());
    }

    @Override
    public LongStream mapToLong(IntToLongFunction mapper) {
        return DebugLongStream.mapFromObject(this, mapper::applyAsLong);
    }

    @Override
    public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
        return DebugDoubleStream.mapFromObject(this, mapper::applyAsDouble);
    }

    @Override
    public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
        return this.operate(s -> s.flatMap(mapper));
    }

    @Override
    public IntStream distinct() {
        return this.operate(IntStream::distinct);
    }

    @Override
    public IntStream sorted() {
        return this.operate(IntStream::sorted);
    }

    @Override
    public IntStream peek(IntConsumer action) {
        return this.operate(s -> s.peek(action));
    }

    @Override
    public IntStream limit(long maxSize) {
        return null;
    }

    @Override
    public IntStream skip(long n) {
        return null;
    }

    @Override
    public void forEach(IntConsumer action) {
        this.operateVoid(s -> s.forEach(action));
    }

    @Override
    public void forEachOrdered(IntConsumer action) {
        this.operateVoid(s -> s.forEachOrdered(action));
    }

    @Override
    public int[] toArray() {
        return this.operate(IntStream::toArray);
    }

    @Override
    public int reduce(int identity, IntBinaryOperator op) {
        return this.operate(s -> s.reduce(identity, op));
    }

    @Override
    public OptionalInt reduce(IntBinaryOperator op) {
        return this.operate(s -> s.reduce(op));
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return this.operate(s -> s.collect(supplier, accumulator, combiner));
    }

    @Override
    public int sum() {
        return this.operate(IntStream::sum);
    }

    @Override
    public OptionalInt min() {
        return this.operate(IntStream::min);
    }

    @Override
    public OptionalInt max() {
        return this.operate(IntStream::max);
    }

    @Override
    public long count() {
        return this.operate(IntStream::count);
    }

    @Override
    public OptionalDouble average() {
        return this.operate(IntStream::average);
    }

    @Override
    public IntSummaryStatistics summaryStatistics() {
        return this.operate(IntStream::summaryStatistics);
    }

    @Override
    public boolean anyMatch(IntPredicate predicate) {
        return this.operate(s -> s.anyMatch(predicate));
    }

    @Override
    public boolean allMatch(IntPredicate predicate) {
        return this.operate(s -> s.allMatch(predicate));
    }

    @Override
    public boolean noneMatch(IntPredicate predicate) {
        return this.operate(s -> s.noneMatch(predicate));
    }

    @Override
    public OptionalInt findFirst() {
        return this.operate(IntStream::findFirst);
    }

    @Override
    public OptionalInt findAny() {
        return this.operate(IntStream::findAny);
    }

    @Override
    public LongStream asLongStream() {
        return null;
    }

    @Override
    public DoubleStream asDoubleStream() {
        return this.operate(IntStream::asDoubleStream);
    }

    @Override
    public Stream<Integer> boxed() {
        return this.operate(IntStream::boxed);
    }

    @Override
    public IntStream sequential() {
        return this.operate(IntStream::sequential);
    }

    @Override
    public IntStream parallel() {
        return this.operate(IntStream::parallel);
    }

    @Override
    public IntStream unordered() {
        return this.operate(IntStream::unordered);
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        return this.operate(s -> s.onClose(closeHandler));
    }

    @Override
    public void close() {
        this.operateVoid(IntStream::close);
    }

    @Override
    public OfInt iterator() {
        return this.operate(IntStream::iterator);
    }

    @Override
    public Spliterator.OfInt spliterator() {
        return this.operate(IntStream::spliterator);
    }

    @Override
    public boolean isParallel() {
        return this.operate(IntStream::isParallel);
    }

    @Override
    public Stream<Integer> getRawStream() {
        return this.stream.boxed();
    }

    @Override
    public boolean isOutputtingResults() {
        return this.outputResults;
    }

}
