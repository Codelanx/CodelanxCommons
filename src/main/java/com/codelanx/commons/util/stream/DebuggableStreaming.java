package com.codelanx.commons.util.stream;

import java.util.stream.Stream;

public interface DebuggableStreaming<T> {

    public Stream<T> getRawStream();

    public boolean isOutputtingResults();

}
