package com.codelanx.commons.util.stream;

import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator.OfLong;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A debuggable and extendable {@link LongStream} wrapper, which allows overriding of only
 * two methods to allow seeing stream operation in realtime.
 *
 * @since 0.3.1
 * @version 0.3.1
 */
public class DebugLongStream implements LongStream, DebuggableStreaming<Long> {

    private static final Map<Class<? extends DebugLongStream>, Constructor<? extends DebugLongStream>> CONSTRUCTORS = new HashMap<>();
    private static final Constructor<? extends DebugLongStream> DEFAULT;

    static {
        Constructor<? extends DebugLongStream> set = null;
        try {
            set = DebugLongStream.class.getDeclaredConstructor(LongStream.class, boolean.class);
            set.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        DEFAULT = set;
    }

    private final LongStream stream;
    private final boolean outputResults;
    private final Map<String, Integer> methodCount = new HashMap<>();

    protected DebugLongStream(LongStream stream, boolean outputResults) {
        this.stream = stream;
        this.outputResults = outputResults;
    }

    /**
     * Creates a new {@link DebugLongStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link LongStream} to wrap
     * @return A new {@link DebugLongStream} wrapper of the provided {@link LongStream}
     */
    public static DebugLongStream of(LongStream stream) {
        return DebugLongStream.of(stream, false);
    }

    /**
     * Creates a new {@link DebugLongStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link LongStream} to wrap
     * @param outputResults {@code true} to output the results of the {@link LongStream} as it is operated upon
     * @return A new {@link DebugLongStream} wrapper of the provided {@link LongStream}
     */
    public static DebugLongStream of(LongStream stream, boolean outputResults) {
        return DebugLongStream.of(stream, outputResults, null);
    }

    /**
     * Instantiates a new {@link DebugLongStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugLongStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link LongStream} to create a {@link DebugLongStream} of.
     * @param type The class representing the type of the {@link DebugLongStream} to instantiate
     * @return A new {@link DebugLongStream} of the provided {@link LongStream}
     */
    public static DebugLongStream of(LongStream stream, Class<? extends DebugLongStream> type) {
        return DebugLongStream.of(stream, false, type);
    }

    /**
     * Instantiates a new {@link DebugLongStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugLongStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link LongStream} to create a {@link DebugLongStream} of.
     * @param outputResults {@code true} to output the results of the {@link LongStream} as it is operated upon
     * @param type The class representing the type of the {@link DebugLongStream} to instantiate
     * @return A new {@link DebugLongStream} of the provided {@link LongStream}, or the base class instance of
     *         {@link DebugLongStream} if the type either failed to instantiate or was {@code null}.
     */
    public static DebugLongStream of(LongStream stream, boolean outputResults, Class<? extends DebugLongStream> type) {
        if (type != null && type != DebugLongStream.class) {
            try {
                Constructor<? extends DebugLongStream> cont = CONSTRUCTORS.<Constructor<? extends DebugLongStream>>computeIfAbsent(type, k -> {
                    try {
                        Constructor<? extends DebugLongStream> c = k.getDeclaredConstructor(LongStream.class, boolean.class);
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
                Debugger.error(ex, "Could not instantiate DebugLongStream of type %s, returning default...", type.getName());
                ex.printStackTrace();
            }
        }
        return new DebugLongStream(stream, outputResults);
    }

    /**
     * Overridable method which runs the appropriate {@link Stream} action, and outputs the result as well as
     * which numerical call it is. For example, the third #filter calll will be printed as
     * {@code DebugStream#filter[2]: &lt;result&gt;}. The output of {@code result} will be substringed to a maximum
     * of 50 characters, to keep the console from being spammed with large datasets. Additionally, method names with
     * differing method parameters will be grouped under the same aliases and therefore, the same method "counter".
     * <br><br>
     * Please note the performance impacts of this method, if this is a standard stream operation and result output
     * is enabled, the {@link Stream} will need to be recreated each time.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param action The {@link Function} representing the action taken on the {@link Stream}
     * @param <R> The return type of the action
     * @return The result of the call on the {@link Stream}
     */
    protected <R> R operate(Function<? super LongStream, ? extends R> action) {
        StackTraceElement elm = Reflections.getCaller();
        Integer amount = this.methodCount.compute(elm.getMethodName(), (k, old) -> old == null ? 0 : old + 1);
        R result = action.apply(this.stream);
        String output = null;
        if (this.outputResults) {
            if (result instanceof LongStream) {
                List<Long> objs = ((LongStream) result).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                output = objs.toString();
                result = (R) objs.stream().mapToLong(Long::valueOf);
            } else {
                output = Objects.toString(result);
            }
            output = output.substring(0, Math.min(50, output.length()));
        }
        System.out.println("DebugLongStream#" + elm.getMethodName() + "[" + amount + "]" + (output == null ? "" : ": " + output));
        return result;
    }

    /**
     * Overridable method which runs the appropriate {@link Stream} action, and outputs the result as well as
     * which numerical call it is. Due to this being a void operation on a {@link Consumer}, the result output will
     * always be {@code null}.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @see DebugStream#operate(Function)
     * @param action The {@link Consumer} representing the action taken on the {@link Stream}
     */
    protected void operateVoid(Consumer<? super LongStream> action) {
        this.operate(s -> {
            action.accept(s);
            return null;
        });
    }

    public static <D> DebugLongStream mapFromObject(DebuggableStreaming<D> debugStream, ToLongFunction<? super D> mapper) {
        return DebugLongStream.of(debugStream.getRawStream().mapToLong(mapper), debugStream.isOutputtingResults());
    }

    public static <D> DebugLongStream flatMapFromObject(DebuggableStreaming<D> debugStream, Function<? super D, ? extends LongStream> mapper) {
        return DebugLongStream.of(debugStream.getRawStream().flatMapToLong(mapper), debugStream.isOutputtingResults());
    }

    @Override
    public LongStream filter(LongPredicate predicate) {
        return this.operate(s -> s.filter(predicate));
    }

    @Override
    public LongStream map(LongUnaryOperator mapper) {
        return this.operate(s -> s.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
        return DebugStream.of(this.getRawStream().map(mapper::apply), this.isOutputtingResults());
    }

    @Override
    public IntStream mapToInt(LongToIntFunction mapper) {
        return DebugIntStream.mapFromObject(this, mapper::applyAsInt);
    }

    @Override
    public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
        return DebugDoubleStream.mapFromObject(this, mapper::applyAsDouble);
    }

    @Override
    public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
        return this.operate(s -> s.flatMap(mapper));
    }

    @Override
    public LongStream distinct() {
        return this.operate(LongStream::distinct);
    }

    @Override
    public LongStream sorted() {
        return this.operate(LongStream::sorted);
    }

    @Override
    public LongStream peek(LongConsumer action) {
        return this.operate(s -> s.peek(action));
    }

    @Override
    public LongStream limit(long maxSize) {
        return this.operate(s -> s.limit(maxSize));
    }

    @Override
    public LongStream skip(long n) {
        return this.operate(s -> s.skip(n));
    }

    @Override
    public void forEach(LongConsumer action) {
        this.operateVoid(s -> s.forEach(action));
    }

    @Override
    public void forEachOrdered(LongConsumer action) {
        this.operateVoid(s -> s.forEachOrdered(action));
    }

    @Override
    public long[] toArray() {
        return this.operate(LongStream::toArray);
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        return this.operate(s -> s.reduce(identity, op));
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        return this.operate(s -> s.reduce(op));
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return this.operate(s -> s.collect(supplier, accumulator, combiner));
    }

    @Override
    public long sum() {
        return this.operate(LongStream::sum);
    }

    @Override
    public OptionalLong min() {
        return this.operate(LongStream::min);
    }

    @Override
    public OptionalLong max() {
        return this.operate(LongStream::max);
    }

    @Override
    public long count() {
        return this.operate(LongStream::count);
    }

    @Override
    public OptionalDouble average() {
        return this.operate(LongStream::average);
    }

    @Override
    public LongSummaryStatistics summaryStatistics() {
        return this.operate(LongStream::summaryStatistics);
    }

    @Override
    public boolean anyMatch(LongPredicate predicate) {
        return this.operate(s -> s.anyMatch(predicate));
    }

    @Override
    public boolean allMatch(LongPredicate predicate) {
        return this.operate(s -> s.allMatch(predicate));
    }

    @Override
    public boolean noneMatch(LongPredicate predicate) {
        return this.operate(s -> s.noneMatch(predicate));
    }

    @Override
    public OptionalLong findFirst() {
        return this.operate(LongStream::findFirst);
    }

    @Override
    public OptionalLong findAny() {
        return this.operate(LongStream::findAny);
    }

    @Override
    public DoubleStream asDoubleStream() {
        return this.operate(LongStream::asDoubleStream);
    }

    @Override
    public Stream<Long> boxed() {
        return this.operate(LongStream::boxed);
    }

    @Override
    public LongStream sequential() {
        return this.operate(LongStream::sequential);
    }

    @Override
    public LongStream parallel() {
        return this.operate(LongStream::parallel);
    }

    @Override
    public LongStream unordered() {
        return this.operate(LongStream::unordered);
    }

    @Override
    public LongStream onClose(Runnable closeHandler) {
        return this.operate(s -> s.onClose(closeHandler));
    }

    @Override
    public void close() {
        this.operateVoid(LongStream::close);
    }

    @Override
    public OfLong iterator() {
        return this.operate(LongStream::iterator);
    }

    @Override
    public Spliterator.OfLong spliterator() {
        return this.operate(LongStream::spliterator);
    }

    @Override
    public boolean isParallel() {
        return this.operate(LongStream::isParallel);
    }

    @Override
    public Stream<Long> getRawStream() {
        return this.stream.boxed();
    }

    @Override
    public boolean isOutputtingResults() {
        return this.outputResults;
    }

}
