package com.codelanx.commons.util.stream;

import com.codelanx.commons.logging.Debugger;
import com.codelanx.commons.util.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * A debuggable and extendable {@link Stream} wrapper, which allows overriding of only
 * two methods to allow seeing stream operation in realtime.
 *
 * @since 0.3.1
 * @version 0.3.1
 *
 * @param <T> The type of the Stream being wrapped
 */
public class DebugStream<T> implements Stream<T>, DebuggableStreaming<T> {

    private static final Map<Class<? extends DebugStream>, Constructor<? extends DebugStream>> CONSTRUCTORS = new HashMap<>();
    private static final Constructor<? extends DebugStream> DEFAULT;

    static {
        Constructor<? extends DebugStream> set = null;
        try {
            set = DebugStream.class.getDeclaredConstructor(Stream.class, boolean.class);
            set.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        DEFAULT = set;
    }

    private Stream<T> stream;
    private final boolean outputResults;
    private final Map<String, Integer> methodCount = new HashMap<>();

    protected DebugStream(Stream<T> stream, boolean outputResults) {
        this.stream = stream;
        this.outputResults = outputResults;
    }

    /**
     * Creates a new {@link DebugStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link Stream} to wrap
     * @param <D> The type of the {@link Stream} elements
     * @return A new {@link DebugStream} wrapper of the provided {@link Stream}
     */
    public static <D> DebugStream<D> of(Stream<D> stream) {
        return DebugStream.of(stream, false);
    }

    /**
     * Creates a new {@link DebugStream} using the default type
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link Stream} to wrap
     * @param outputResults {@code true} to output the results of the {@link Stream} as it is operated upon
     * @param <D> The type of the {@link Stream} elements
     * @return A new {@link DebugStream} wrapper of the provided {@link Stream}
     */
    public static <D> DebugStream<D> of(Stream<D> stream, boolean outputResults) {
        return DebugStream.of(stream, outputResults, null);
    }

    /**
     * Instantiates a new {@link DebugStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link Stream} to create a {@link DebugStream} of.
     * @param type The class representing the type of the {@link DebugStream} to instantiate
     * @param <D> The type of the {@link Stream} elements
     * @return A new {@link DebugStream} of the provided {@link Stream}
     */
    public static <D> DebugStream<D> of(Stream<D> stream, Class<? extends DebugStream<D>> type) {
        return DebugStream.of(stream, false, type);
    }

    /**
     * Instantiates a new {@link DebugStream} of the type parameter passed to the method. This is the appropriate
     * way to instantiate the {@link DebugStream}, as future caching mechanisms will be in place to optimize this.
     * You may however expose the constructor if you so choose.
     *
     * @since 0.3.1
     * @version 0.3.1
     *
     * @param stream The {@link Stream} to create a {@link DebugStream} of.
     * @param outputResults {@code true} to output the results of the {@link Stream Stream&lt;T&gt;} as it is operated upon
     * @param type The class representing the type of the {@link DebugStream} to instantiate
     * @param <D> The type of the {@link Stream} elements
     * @return A new {@link DebugStream} of the provided {@link Stream}, or the base class instance of
     *         {@link DebugStream} if the type either failed to instantiate or was {@code null}.
     */
    public static <D> DebugStream<D> of(Stream<D> stream, boolean outputResults, Class<? extends DebugStream> type) {
        if (type != null && type != DebugStream.class) {
            try {
                Constructor<? extends DebugStream> cont = CONSTRUCTORS.<Constructor<? extends DebugStream<D>>>computeIfAbsent(type, k -> {
                    try {
                        Constructor<? extends DebugStream> c = k.getDeclaredConstructor(Stream.class, boolean.class);
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
                Debugger.error(ex, "Could not instantiate DebugStream of type %s, returning default...", type.getName());
                ex.printStackTrace();
            }
        }
        return new DebugStream<>(stream, outputResults);
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
    protected <R> R operate(Function<? super Stream<T>, ? extends R> action) {
        StackTraceElement elm = Reflections.getCaller();
        Integer amount = this.methodCount.compute(elm.getMethodName(), (k, old) -> old == null ? 0 : old + 1);
        R result = action.apply(this.stream);
        String output = null;
        if (this.outputResults) {
            if (result instanceof Stream) {
                List<Object> objs = (List<Object>) ((Stream) result).collect(Collectors.toList());
                output = objs.toString();
                result = (R) objs.stream();
            } else {
                output = Objects.toString(result);
            }
            output = output.substring(0, Math.min(50, output.length()));
        }
        System.out.println("DebugStream#" + elm.getMethodName() + "[" + amount + "]" + (output == null ? "" : ": " + output));
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
    protected void operateVoid(Consumer<? super Stream<T>> action) {
        this.operate(s -> {
            action.accept(s);
            return null;
        });
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        this.stream = this.operate(s -> s.filter(predicate));
        return this;
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return DebugStream.of(this.operate(s -> s.map(mapper)), this.outputResults, this.getClass());
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return DebugIntStream.mapFromObject(this, mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return DebugLongStream.mapFromObject(this, mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return DebugDoubleStream.mapFromObject(this, mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return DebugStream.of(this.operate(s -> s.flatMap(mapper)), this.outputResults, this.getClass());
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return DebugIntStream.flatMapFromObject(this, mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return DebugLongStream.flatMapFromObject(this, mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return DebugDoubleStream.flatMapFromObject(this, mapper);
    }

    @Override
    public Stream<T> distinct() {
        this.stream = this.operate(Stream::distinct);
        return this;
    }

    @Override
    public Stream<T> sorted() {
        this.stream = this.operate(Stream::sorted);
        return this;
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        this.stream = this.operate(s -> s.sorted(comparator));
        return this;
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        this.stream = this.operate(s -> s.peek(action));
        return this;
    }

    @Override
    public Stream<T> limit(long maxSize) {
        this.stream = this.operate(s -> s.limit(maxSize));
        return this;
    }

    @Override
    public Stream<T> skip(long n) {
        this.stream = this.operate(s -> s.skip(n));
        return this;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        this.operateVoid(s -> s.forEach(action));
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        this.operateVoid(s -> s.forEachOrdered(action));
    }

    @Override
    public Object[] toArray() {
        return this.operate(Stream::toArray);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return this.operate(s -> s.toArray(generator));
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return this.operate(s -> s.reduce(identity, accumulator));
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return this.operate(s -> s.reduce(accumulator));
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return this.operate(s -> s.reduce(identity, accumulator, combiner));
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return this.operate(s -> s.collect(supplier, accumulator, combiner));
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return this.operate(s -> s.collect(collector));
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return this.operate(s -> s.min(comparator));
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return this.operate(s -> s.max(comparator));
    }

    @Override
    public long count() {
        return this.operate(Stream::count);
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return this.operate(s -> s.anyMatch(predicate));
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return this.operate(s -> s.allMatch(predicate));
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return this.operate(s -> s.noneMatch(predicate));
    }

    @Override
    public Optional<T> findFirst() {
        return this.operate(Stream::findFirst);
    }

    @Override
    public Optional<T> findAny() {
        return this.operate(Stream::findAny);
    }

    @Override
    public Iterator<T> iterator() {
        return this.operate(Stream::iterator);
    }

    @Override
    public Spliterator<T> spliterator() {
        return this.operate(Stream::spliterator);
    }

    @Override
    public boolean isParallel() {
        return this.operate(Stream::isParallel);
    }

    @Override
    public Stream<T> sequential() {
        this.stream = this.operate(Stream::sequential);
        return this;
    }

    @Override
    public Stream<T> parallel() {
        this.stream = this.operate(Stream::parallel);
        return this;
    }

    @Override
    public Stream<T> unordered() {
        this.stream = this.operate(Stream::unordered);
        return this;
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        this.stream = this.operate(s -> s.onClose(closeHandler));
        return this;
    }

    @Override
    public void close() {
        this.operateVoid(Stream::close);
    }

    @Override
    public Stream<T> getRawStream() {
        return this.stream;
    }

    @Override
    public boolean isOutputtingResults() {
        return this.outputResults;
    }

}
