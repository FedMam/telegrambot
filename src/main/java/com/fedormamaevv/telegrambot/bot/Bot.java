package com.fedormamaevv.telegrambot.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class Bot extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botUsername;

    @Value("${bot.token}")
    private String botToken;

    private enum BotCommands { idle, precision, triangle, tri_3s, tri_2s, tri_1s, sq_eq }
    private BotCommands state;
    private int precision = 5;

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (!update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText()) {
                final Message message = update.getMessage();
                String msg = message.getText();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(String.valueOf(update.getMessage().getChatId()));
                sendMessage.setText("БИП -- ОШИБКА\nКоманда не поддерживается\n/about: список команд для меня");
                if (msg.compareTo("/start") == 0 || (msg.compareTo("/about") == 0 && state == BotCommands.idle))
                {
                    state = BotCommands.idle;
                    sendMessage.setText("Привет! Я FMMathBot. Что я умею:\n" +
                            "/triangle - посчитать стороны, углы и площадь треугольника по 3 параметрам\n" +
                            "/equation - посчитать корни квадратного уравнения\n" +
                            "/precision - установить точность (по умолчанию 5)\n" +
                            "/about - обо мне");
                }
                else if (state == BotCommands.precision)
                {
                    try
                    {
                        precision = Integer.parseInt(msg);
                        sendMessage.setText("Точность установлена в " + precision);
                        state = BotCommands.idle;
                    }
                    catch (Exception e)
                    {
                        sendMessage.setText("БИП -- ОШИБКА\n" + e.getMessage()
                                + "\nВведите точность (число знаков после запятой):");
                    }
                }
                else if (state == BotCommands.idle)
                {
                    if (msg.compareTo("/equation") == 0)
                    {
                        sendMessage.setText("Квадратное уравнение:\nax² + bx + c = 0\n" +
                                "Введите через пробел 3 числа:\na, b и c");
                        state = BotCommands.sq_eq;
                    }
                    else if (msg.compareTo("/precision") == 0)
                    {
                        sendMessage.setText("Введите точность (число знаков после запятой):");
                        state = BotCommands.precision;
                    }
                    else if (msg.compareTo("/triangle") == 0)
                    {
                        sendMessage.setText("Введите через пробел длины 3 сторон:");
                        state = BotCommands.triangle;
                    }
                    else if (msg.compareTo("/cancel") == 0)
                    {
                        state = BotCommands.idle;
                    }
                }
                // -----
                else if (state == BotCommands.triangle)
                {
                    try
                    {
                        double a, b, c;
                        String[] par = msg.replaceAll(",", ".").split(" ");
                        a = Double.parseDouble(par[0]);
                        b = Double.parseDouble(par[1]);
                        c = Double.parseDouble(par[2]);
                        sendMessage.setText("Углы (в градусах): " + round(angle(a, b, c)) + ", " + round(angle(b, a, c)) + ", " + round(angle(c, b, a))
                        + "\nПлощадь: " + round(squareArea(a, b, c)));

                    }
                    catch (ArithmeticException ie)
                    {
                        sendMessage.setText("БИП -- ОШИБКА\nЭто не треугольник\nВведите через пробел длины 3 сторон:");
                    }
                    catch (Exception e)
                    {
                        sendMessage.setText("БИП -- ОШИБКА\n" +
                                e.getMessage() +  "\nВведите через пробел длины 3 сторон:");
                    }
                    state = BotCommands.idle;
                }
                // -----
                else if (state == BotCommands.sq_eq)
                {
                    try
                    {
                        double a, b, c;
                        String[] par = msg.replaceAll(",", ".").split(" ");
                        a = Double.parseDouble(par[0]);
                        b = Double.parseDouble(par[1]);
                        c = Double.parseDouble(par[2]);
                        double D = b * b - 4 * a * c;
                        if (Double.compare(D, 0.0) == -1)
                        {
                            sendMessage.setText("Вещественных корней нет");
                        }
                        else if (Double.compare(D, 0.0) == 0)
                        {
                            sendMessage.setText("1 вещественный корень: " + round(-b / (2*a)));
                        }
                        else
                        {
                            double rt = Math.sqrt(D);
                            sendMessage.setText("2 вещественных корня: " + round((-b - rt) / (2*a)) + ", " + round((-b + rt) / (2*a)));
                        }
                        state = BotCommands.idle;
                    }
                    catch (Exception e)
                    {
                        sendMessage.setText("БИП -- ОШИБКА\n" + e.getMessage() + "\nВведите через пробел 3 числа: a, b и c");
                    }
                }
                else
                {
                    sendMessage.setText("БИП -- ОШИБКА\nКоманда не поддерживается\n/about: список команд для меня");
                }
                execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private double squareArea(double a, double b, double c)
        throws ArithmeticException {
        double p = (a + b + c) / 2;
        double result = Math.sqrt(p * (p - a) * (p - b) * (p - c));
        if (Double.isNaN(result)) throw new ArithmeticException();
        return result;
    }

    private double angle(double opp, double a, double b)
        throws ArithmeticException {
        // opp2 = a2+b2-2ab cos ang
        // ang = arccos((a2+b2-opp2)/2ab)
        double result = Math.acos((a*a+b*b-opp*opp)/(2*a*b));
        if (Double.isNaN(result)) throw new ArithmeticException();
        return (result / Math.PI * 180);
    }

    private double round(double v) {
        return (double)Math.round(v * Math.pow(10, precision)) / Math.pow(10, precision);
    }

    public String getBotUsername() { return botUsername; }
    public String getBotToken() { return botToken; }
}
