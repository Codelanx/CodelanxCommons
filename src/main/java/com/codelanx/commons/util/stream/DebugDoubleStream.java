package com.codelanx.commons.util.stream;

import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator.OfDouble;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleToIntFunction;
import java.util.function.Function;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A debuggable and extendable {@link DoubleStream} wrapper, which allows overriding of only
 * two methods to allow seeing stream operation in realtime.
 *
 * @since 0.3.1
 * @version 0.3.1
 */
public class DebugDoubleStream implements DoubleStream, DebuggableStreaming<Double> {

    private static final Map<Class<? extends DebugDoubleStream>, Constructor<? extends DebugDoubleStream>> CONSTRUCTORS = new HashMap<>();
    private static final Constructor<? extends DebugDoubleStream> DEFAULT;

    static {
        Constructor<? extends DebugDoubleStream> set = null;
        try {
            set = DebugDoubleStream.class.getDeclaredConstructor(DoubleStream.class, boolean.class);
            set.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        DEFAULT = set;
    }

    private final DoubleStream stream;
    private final boolean outputResults;
    private final Map<String, Integer> methodCount = new HashMap<>();

    protected DebugDoubleStream(DoubleStream stream, boolean outputResults) {
        this.stream = stream;
        this.outputResults = outputResults;
    }

    /**
     * Creates a new {@link DebugDoubleStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link DoubleStream} to wrap
     * @return A new {@link DebugDoubleStream} wrapper of the provided {@link DoubleStream}
     */
    public static DebugDoubleStream of(DoubleStream stream) {
        return DebugDoubleStream.of(stream, false);
    }

    /**
     * Creates a new {@link DebugDoubleStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link DoubleStream} to wrap
     * @param outputResults {@code true} to output the results of the {@link DoubleStream} as it is operated upon
     * @return A new {@link DebugDoubleStream} wrapper of the provided {@link DoubleStream}
     */
    public static DebugDoubleStream of(DoubleStream stream, boolean outputResults) {
        return DebugDoubleStream.of(stream, outputResults, null);
    }

    /**
     * Instantiates a new {@link DebugDoubleStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugDoubleStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link DoubleStream} to create a {@link DebugDoubleStream} of.
     * @param type The class representing the type of the {@link DebugDoubleStream} to instantiate
     * @return A new {@link DebugDoubleStream} of the provided {@link DoubleStream}
     */
    public static DebugDoubleStream of(DoubleStream stream, Class<? extends DebugDoubleStream> type) {
        return DebugDoubleStream.of(stream, false, type);
    }

    /**
     * Instantiates a new {@link DebugDoubleStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugDoubleStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link DoubleStream} to create a {@link DebugDoubleStream} of.
     * @param type The class representing the type of the {@link DebugDoubleStream} to instantiate
     * @return A new {@link DebugDoubleStream} of the provided {@link DoubleStream}, or the base class instance of
     *         {@link DebugDoubleStream} if the type either failed to instantiate or was {@code null}.
     */
    public static DebugDoubleStream of(DoubleStream stream, boolean outputResults, Class<? extends DebugDoubleStream> type) {
        if (type != null && type != DebugDoubleStream.class) {
            try {
                Constructor<? extends DebugDoubleStream> cont = CONSTRUCTORS.<Constructor<? extends DebugDoubleStream>>computeIfAbsent(type, k -> {
                    try {
                        Constructor<? extends DebugDoubleStream> c = k.getDeclaredConstructor(DoubleStream.class, boolean.class);
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
                Debugger.error(ex, "Could not instantiate DebugDoubleStream of type %s, returning default...", type.getName());
                ex.printStackTrace();
            }
        }
        return new DebugDoubleStream(stream, outputResults);
    }

    /**
     * Overridable method which runs the appropriate {@link DoubleStream} action, and outputs the result as well as
     * which numerical call it is. For example, the third #filter calll will be printed as
     * {@code DebugStream#filter[2]: &lt;result&gt;}. The output of {@code result} will be substringed to a maximum
     * of 50 characters, to keep the console from being spammed with large datasets. Additionally, method names with
     * differing method parameters will be grouped under the same aliases and therefore, the same method "counter".
     * <br /><br />
     * Please note the performance impacts of this method, if this is a standard stream operation and result output
     * is enabled, the {@link DoubleStream} will need to be recreated each time.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param action The {@link Function} representing the action taken on the {@link DoubleStream}
     * @param <R> The return type of the action
     * @return The result of the call on the {@link DoubleStream}
     */
    protected <R> R operate(Function<? super DoubleStream, ? extends R> action) {
        StackTraceElement elm = Reflections.getCaller();
        Integer amount = this.methodCount.compute(elm.getMethodName(), (k, old) -> old == null ? 0 : old + 1);
        R result = action.apply(this.stream);
        String output = null;
        if (this.outputResults) {
            if (result instanceof DoubleStream) {
                List<Double> objs = ((DoubleStream) result).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                output = objs.toString();
                result = (R) objs.stream().mapToDouble(Double::valueOf);
            } else {
                output = Objects.toString(result);
            }
            output = output.substring(0, Math.min(50, output.length()));
        }
        System.out.println("DebugLongStream#" + elm.getMethodName() + "[" + amount + "]" + (output == null ? "" : ": " + output));
        return result;
    }

    /**
     * Overridable method which runs the appropriate {@link DoubleStream} action, and outputs the result as well as
     * which numerical call it is. Due to this being a void operation on a {@link Consumer}, the result output will
     * always be {@code null}.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @see DebugStream#operate(Function)
     * @param action The {@link Consumer} representing the action taken on the {@link DoubleStream}
     */
    protected void operateVoid(Consumer<? super DoubleStream> action) {
        this.operate(s -> {
            action.accept(s);
            return null;
        });
    }

    public static <D> DebugDoubleStream mapFromObject(DebuggableStreaming<D> debugStream, ToDoubleFunction<? super D> mapper) {
        return DebugDoubleStream.of(debugStream.getRawStream().mapToDouble(mapper), debugStream.isOutputtingResults());
    }

    public static <D> DebugDoubleStream flatMapFromObject(DebuggableStreaming<D> debugStream, Function<? super D, ? extends DoubleStream> mapper) {
        return DebugDoubleStream.of(debugStream.getRawStream().flatMapToDouble(mapper), debugStream.isOutputtingResults());
    }

    @Override
    public DoubleStream filter(DoublePredicate predicate) {
        return this.operate(s -> s.filter(predicate));
    }

    @Override
    public DoubleStream map(DoubleUnaryOperator mapper) {
        return this.operate(s -> s.map(mapper));
    }

    @Override
    public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
        return DebugStream.of(this.getRawStream().map(mapper::apply), this.isOutputtingResults());
    }

    @Override
    public LongStream mapToLong(DoubleToLongFunction mapper) {
        return DebugLongStream.mapFromObject(this, mapper::applyAsLong);
    }

    @Override
    public IntStream mapToInt(DoubleToIntFunction mapper) {
        return DebugIntStream.mapFromObject(this, mapper::applyAsInt);
    }

    @Override
    public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
        return this.operate(s -> s.flatMap(mapper));
    }

    @Override
    public DoubleStream distinct() {
        return this.operate(DoubleStream::distinct);
    }

    @Override
    public DoubleStream sorted() {
        return this.operate(DoubleStream::sorted);
    }

    @Override
    public DoubleStream peek(DoubleConsumer action) {
        return this.operate(s -> s.peek(action));
    }

    @Override
    public DoubleStream limit(long maxSize) {
        return null;
    }

    @Override
    public DoubleStream skip(long n) {
        return null;
    }

    @Override
    public void forEach(DoubleConsumer action) {
        this.operateVoid(s -> s.forEach(action));
    }

    @Override
    public void forEachOrdered(DoubleConsumer action) {
        this.operateVoid(s -> s.forEachOrdered(action));
    }

    @Override
    public double[] toArray() {
        return this.operate(DoubleStream::toArray);
    }

    @Override
    public double reduce(double identity, DoubleBinaryOperator op) {
        return this.operate(s -> s.reduce(identity, op));
    }

    @Override
    public OptionalDouble reduce(DoubleBinaryOperator op) {
        return this.operate(s -> s.reduce(op));
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
        return this.operate(s -> s.collect(supplier, accumulator, combiner));
    }

    @Override
    public double sum() {
        return this.operate(DoubleStream::sum);
    }

    @Override
    public OptionalDouble min() {
        return this.operate(DoubleStream::min);
    }

    @Override
    public OptionalDouble max() {
        return this.operate(DoubleStream::max);
    }

    @Override
    public long count() {
        return this.operate(DoubleStream::count);
    }

    @Override
    public OptionalDouble average() {
        return this.operate(DoubleStream::average);
    }

    @Override
    public DoubleSummaryStatistics summaryStatistics() {
        return this.operate(DoubleStream::summaryStatistics);
    }

    @Override
    public boolean anyMatch(DoublePredicate predicate) {
        return this.operate(s -> s.anyMatch(predicate));
    }

    @Override
    public boolean allMatch(DoublePredicate predicate) {
        return this.operate(s -> s.allMatch(predicate));
    }

    @Override
    public boolean noneMatch(DoublePredicate predicate) {
        return this.operate(s -> s.noneMatch(predicate));
    }

    @Override
    public OptionalDouble findFirst() {
        return this.operate(DoubleStream::findFirst);
    }

    @Override
    public OptionalDouble findAny() {
        return this.operate(DoubleStream::findAny);
    }

    @Override
    public Stream<Double> boxed() {
        return this.operate(DoubleStream::boxed);
    }

    @Override
    public DoubleStream sequential() {
        return this.operate(DoubleStream::sequential);
    }

    @Override
    public DoubleStream parallel() {
        return this.operate(DoubleStream::parallel);
    }

    @Override
    public DoubleStream unordered() {
        return this.operate(DoubleStream::unordered);
    }

    @Override
    public DoubleStream onClose(Runnable closeHandler) {
        return this.operate(s -> s.onClose(closeHandler));
    }

    @Override
    public void close() {
        this.operateVoid(DoubleStream::close);
    }

    @Override
    public OfDouble iterator() {
        return this.operate(DoubleStream::iterator);
    }

    @Override
    public Spliterator.OfDouble spliterator() {
        return this.operate(DoubleStream::spliterator);
    }

    @Override
    public boolean isParallel() {
        return this.operate(DoubleStream::isParallel);
    }

    @Override
    public Stream<Double> getRawStream() {
        return this.stream.boxed();
    }

    @Override
    public boolean isOutputtingResults() {
        return this.outputResults;
    }

}
