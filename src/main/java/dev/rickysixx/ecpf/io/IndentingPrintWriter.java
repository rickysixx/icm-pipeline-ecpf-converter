package dev.rickysixx.ecpf.io;

import java.io.PrintWriter;

public class IndentingPrintWriter
{
    private final PrintWriter writer;

    private int currentIndentationLevel;

    private static final int INDENTATION_LEVEL_STEP = 2;

    private String indentation()
    {
        return " ".repeat(currentIndentationLevel);
    }

    public IndentingPrintWriter(PrintWriter writer)
    {
        this.writer = writer;
        this.currentIndentationLevel = 0;
    }

    public void increaseIndentationLevel()
    {
        this.currentIndentationLevel += INDENTATION_LEVEL_STEP;
    }

    public void decreaseIndentationLevel()
    {
        this.currentIndentationLevel -= INDENTATION_LEVEL_STEP;
    }

    public void indentedBlock(Runnable codeBlock)
    {
        increaseIndentationLevel();

        codeBlock.run();

        decreaseIndentationLevel();
    }

    public void println(String message)
    {
        writer.println(indentation() + message);
    }

    public void printf(String format, Object... args)
    {
        writer.printf(indentation() + format, args);
    }
}
