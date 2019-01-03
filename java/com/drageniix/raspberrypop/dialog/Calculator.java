package com.drageniix.raspberrypop.dialog;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.drageniix.raspberrypop.R;
import com.drageniix.raspberrypop.activities.BaseActivity;
import com.drageniix.raspberrypop.utilities.Logger;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class Calculator {
    private static final String ADD = "+";
    private static final String SUB = "-";
    private static final String MUL = "\u00D7";
    private static final String DIV = "\u00F7";
    private static final String EXP = "^";
    private static final String OB = "(";
    private static final String CB = ")";

    private static final String X = "x";
    private static final String BACK = "←";
    private static final String CLEAR = "c";

    private String delimiter;

    Calculator(String delimiter){
        this.delimiter = delimiter;
    }
    
    void setCalculator(final BaseActivity activity, final EditText customID){
        View view = View.inflate(activity, R.layout.media_calculator, null);
        final EditText equationInput = view.findViewById(R.id.equation);
        equationInput.setLetterSpacing(0.15f);
        equationInput.setText(format(customID.getText().toString()));
        if (equationInput.getText().toString().isEmpty()){
            equationInput.setText(String.valueOf(X + "+1"));
        }

        String text = equationInput.getText().toString();
        Spannable spannable = new SpannableString(text);
        for(int i = 0; i < text.length(); i++){
            if (Character.toString(text.charAt(i)).equals(X)){
                spannable.setSpan(new ForegroundColorSpan(activity.getAttributeColor(R.attr.colorAccent2)), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        equationInput.setText(spannable, TextView.BufferType.SPANNABLE);
        equationInput.setSelection(equationInput.length());

        Button current = view.findViewById(R.id.current),
                clear = view.findViewById(R.id.clear), backspace = view.findViewById(R.id.backspace),
                add = view.findViewById(R.id.addition), subtract = view.findViewById(R.id.subtract),
                divide = view.findViewById(R.id.divide), multiply = view.findViewById(R.id.multiply),
                obracket = view.findViewById(R.id.obracket), cbracket = view.findViewById(R.id.cbracket),
                period = view.findViewById(R.id.period),
                zero = view.findViewById(R.id.zero), one = view.findViewById(R.id.one), two = view.findViewById(R.id.two), three = view.findViewById(R.id.three),
                four = view.findViewById(R.id.four), five = view.findViewById(R.id.five), six = view.findViewById(R.id.six), seven = view.findViewById(R.id.seven),
                eight = view.findViewById(R.id.eight), nine = view.findViewById(R.id.nine);

        clear.setText(CLEAR);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                equationInput.getText().clear();
            }
        });

        backspace.setText(BACK);
        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = Math.max(equationInput.getSelectionStart(), 0);
                int end = Math.max(equationInput.getSelectionEnd(), 0);
                if (end - start == 0 && start != 0){
                    end = start;
                    start -= 1;
                }
                equationInput.getText().delete(start, end);

            }
        });

        //x value
        current.setText(X);
        current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = equationInput.getSelectionStart();
                equationInput.getText().insert(index, ((Button)v).getText().toString());
                String text = equationInput.getText().toString();
                Spannable spannable = new SpannableString(text);
                for(int i = 0; i < text.length(); i++){
                    if (Character.toString(text.charAt(i)).equals(X)){
                        spannable.setSpan(new ForegroundColorSpan(activity.getAttributeColor(R.attr.colorAccent2)), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannable.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                equationInput.setText(spannable, TextView.BufferType.SPANNABLE);
                equationInput.setSelection(index + 1);
            }
        });


        View.OnClickListener number = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                equationInput.getText().insert(equationInput.getSelectionStart(), ((Button)v).getText().toString());
            }
        };

        //Digits
        zero.setOnClickListener(number);
        one.setOnClickListener(number);
        two.setOnClickListener(number);
        three.setOnClickListener(number);
        four.setOnClickListener(number);
        five.setOnClickListener(number);
        six.setOnClickListener(number);
        seven.setOnClickListener(number);
        eight.setOnClickListener(number);
        nine.setOnClickListener(number);
        period.setOnClickListener(number);

        //Operands
        add.setText(ADD);
        add.setOnClickListener(number);
        subtract.setText(SUB);
        subtract.setOnClickListener(number);
        multiply.setText(MUL);
        multiply.setOnClickListener(number);
        divide.setText(DIV);
        divide.setOnClickListener(number);
        obracket.setText(OB);
        obracket.setOnClickListener(number);
        cbracket.setText(CB);
        cbracket.setOnClickListener(number);

        new AlertDialog.Builder(activity)
                .setView(view)
                .setPositiveButton(activity.getString(R.string.submit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final List<String> equation = new LinkedList<>();
                        String equationText = equationInput.getText().toString();
                        StringBuilder number = new StringBuilder();
                        for(int i = 0; i < equationText.length(); i++){
                            String character = String.valueOf(equationText.charAt(i));
                            if (isOperand(character) || character.equals(X)){
                                if (i != 0) {
                                    String prev = String.valueOf(equationText.charAt(i-1));
                                    if (((character.equals(OB) || character.equals(X))
                                            && (prev.equals(CB) || !isOperand(prev)))) {
                                        appendNumber(equation, number);
                                        equation.add(MUL);
                                        equation.add(character);
                                    } else{
                                        appendNumber(equation, number);
                                        equation.add(character);
                                    }
                                } else {
                                    appendNumber(equation, number);
                                    equation.add(character);
                                }
                            } else {
                                if (i != 0) {
                                    String prev = String.valueOf(equationText.charAt(i-1));
                                    if (prev.equals(CB) || prev.equals(X)) {
                                        equation.add(MUL);
                                    }
                                }
                                number.append(character);
                            }
                        }
                        appendNumber(equation, number);
                        customID.setText(TextUtils.join(delimiter, equation));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create()
                .show();
    }

    private void appendNumber(List<String> equation, StringBuilder number){
        String current = number.toString();
        if (!current.isEmpty()){
            if (current.equals(X)){
                equation.add(current);
            } else try {
                BigDecimal figure = new BigDecimal(current);
                equation.add(String.valueOf(figure.doubleValue()));
                number.setLength(0);
            } catch (Exception e){
                Logger.log(Logger.API, e);
                number.setLength(0);
            }
        }
    }

    public String format(String value){
        return value.replaceAll("\\.0(\\s|$)", " ") //traling 0s
                .replaceAll("(" + X + "|\\d+) " + MUL + " \\(", "$1(") //d(
                .replaceAll(" \\) " + MUL + " (" + X + "|\\d+)", ")$1") //)d
                .replaceAll("(\\d+|" + X + ") " + MUL + " x", " $1x") //dx
                .replaceAll("\\s+", ""); //spaces
    }

    public BigDecimal handleCalculation(String current, String equation) {
        Stack<BigDecimal> stack = new Stack<>();

        try {
            BigDecimal operand1, operand2;
            equation = equation.replace(X, String.valueOf(new BigDecimal(current).doubleValue())).trim();
            String[] input = infixToPostfix(equation, delimiter).split(delimiter);

            for (String token : input) {
                if (isOperand(token)) {
                    operand2 = stack.pop();
                    operand1 = stack.pop();
                    BigDecimal local = null;
                    switch (token) {
                        case ADD:
                            local = operand1.add(operand2);
                            break;
                        case SUB:
                            local = operand1.subtract(operand2);
                            break;
                        case MUL:
                            local = operand1.multiply(operand2);
                            break;
                        case DIV:
                            local = operand1.divide(operand2, BigDecimal.ROUND_HALF_EVEN);
                            break;
                        case EXP:
                            local = new BigDecimal(Math.pow(operand1.doubleValue(), operand2.doubleValue()));
                            break;
                    }
                    stack.push(local);
                } else {
                    stack.push(new BigDecimal(token));
                }
            }
        } catch (Exception e){
            Logger.log(Logger.API, e);
        }

        return stack.size() == 1 ? stack.pop() : null;
    }

    private String infixToPostfix(String input, String delimiter) {
        Stack<String> stack = new Stack<>();
        StringBuilder result = new StringBuilder();
        String[] st = input.split(delimiter);
        for (String token : st) {
            if (isOperand(token)) {
                if (CB.equals(token)) {
                    while (!stack.isEmpty() && !OB.equals(stack.peek())) {
                        result.append(stack.pop()).append(delimiter);
                    }
                    if (!stack.isEmpty()) {
                        stack.pop();
                    }
                } else {
                    if (!stack.isEmpty() && !isLowerPrecedence(token, stack.peek())) {
                        stack.push(token);
                    } else {
                        while (!stack.isEmpty() && isLowerPrecedence(token, stack.peek())) {
                            String top = stack.pop();
                            if (!OB.equals(top)) {
                                result.append(top).append(delimiter);
                            }
                        }
                        stack.push(token);
                    }
                }
            } else {
                result.append(token).append(delimiter);
            }
        }
        while (!stack.isEmpty()) {
            result.append(stack.pop()).append(delimiter);
        }

        return result.toString().replace(OB, "").replace(CB, "").replaceAll("\\s{2,}", " ").trim();
    }

    private boolean isLowerPrecedence(String current, String next) {
        switch (current) {
            case ADD:
                return !(ADD.equals(next) || OB.equals(next));
            case SUB:
                return !(SUB.equals(next) || OB.equals(next));
            case MUL:
                return DIV.equals(next) || EXP.equals(next) || OB.equals(next);
            case DIV:
                return MUL.equals(next) || EXP.equals(next) || OB.equals(next);
            case EXP:
                return OB.equals(next);
            case OB:
            default:
                return false;
        }
    }

    private boolean isOperand(String token){
        return token.equals(ADD) || token.equals(SUB) ||
                token.equals(MUL) || token.equals(DIV) ||
                token.equals(EXP) ||
                token.equals(OB) || token.equals(CB);
    }
}
