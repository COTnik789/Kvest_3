package com.javarush;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/quest")
public class QuestServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        out.println("<html><body>");
        out.println("<h1>Добро пожаловать в текстовый квест!</h1>");
        out.println("<form action='quest' method='POST'>");
        out.println("Введите ваше имя: <input type='text' name='username'>");
        out.println("<input type='submit' value='Начать квест'>");
        out.println("</form>");
        out.println("</body></html>");
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");

        // Проверяем, пустое ли имя или состоит только из пробелов
        if (username == null || username.trim().isEmpty()) {
            username = "выживший"; // Устанавливаем имя по умолчанию
        }

        // Сохраняем имя пользователя в сессии
        HttpSession session = request.getSession();
        session.setAttribute("username", username);
        String action = request.getParameter("action");

        PrintWriter out = response.getWriter();
        out.println("<html><body>");

        Integer health = (Integer) session.getAttribute("health");
        if (health == null) {
            health = 100;
            session.setAttribute("health", health);
        }

        Set<String> inventory = (Set<String>) session.getAttribute("inventory");
        if (inventory == null) {
            inventory = new HashSet<>();
            session.setAttribute("inventory", inventory);
        }

        Map<String, Integer> resources = (Map<String, Integer>) session.getAttribute("resources");
        if (resources == null) {
            resources = new HashMap<>();
            session.setAttribute("resources", resources);
        }

        if (action == null) {
            // Приветствие и первый выбор
            out.println("<h1>Привет, " + username + "!</h1>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            out.println("<p>Вы оказались в густом лесу после авиакатастрофы. Вокруг темнеет, и вы слышите шум неподалеку. Что вы сделаете?</p>");
            out.println("<form action='quest' method='POST'>");
            out.println("<input type='radio' name='action' value='explore'> Пойти на шум<br>");
            out.println("<input type='radio' name='action' value='stay'> Исследовать близжайшую местность<br>");
            out.println("<input type='submit' value='Выбрать'>");
            out.println("</form>");
        } else if ("explore".equals(action)) {
            health -= 30;
            session.setAttribute("health", health);
            out.println("<h1>Вы решаете исследовать шум.</h1>");
            out.println("<p>Следую на звук, вы попадаете в густой кустарник, пытаясь выбраться из него, ранитесь, теряя 30 едениц здоровья</p>");
            out.println("<p>Но вылазка не прошла даром по дороге вы нашли камень и пару палок и решили их подобрать, вполне возможно он вам пригодиться уже скоро</p>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            gatherResources(out, session, "Камень", 2);
            gatherResources(out, session, "Дерево", 2);
            showInventory(out, inventory, resources);
            // Следующее событие: Ночь наступает
            nightEvent(out, session);
        } else if ("stay".equals(action)) {
            out.println("<h1>Вы решаете оставаться на месте и исследовать место авиакатастрофы.</h1>");
            out.println("<p>Вы находите провиант, спички и сигнальный огонь в обломках самолета.</p>");
            inventory.add("Спички");
            inventory.add("Сигнальный огонь");
            inventory.add("Бутылка воды");
            inventory.add("Плитка шоколада");
            session.setAttribute("inventory", inventory);
            out.println("<p>Ваше здоровье: " + health + "</p>");
            gatherResources(out, session, "Дерево", 3);
            // Показать инвентарь
            showInventory(out, inventory, resources);
            // Следующее событие: Ночь наступает
            nightEvent(out, session);
        }
        else if ("craft".equals(action)) {
            // Сохраняем текущий URL как предыдущую страницу
            String referer = request.getHeader("Referer");
            if (referer != null) {
                session.setAttribute("previousPage", referer);
            } else {
                session.setAttribute("previousPage", request.getRequestURI() + "?" + request.getQueryString());
            }
            showCraftingOptions(out, resources, session);
        } else if ("confirm_craft".equals(action)) {
            String itemToCraft = request.getParameter("itemToCraft");
            String craftedItem = craftItem(resources, itemToCraft);
            if (!craftedItem.equals("Невозможно создать предмет")) {
                inventory.add(craftedItem);
                out.println("<h1>Вы создали " + craftedItem + ".</h1>");
            } else {
                out.println("<h1>" + craftedItem + "</h1>");
            }
            showInventory(out, inventory, resources);
            // Следующее событие
            nightEvent(out, session);
            // Обработка выхода из меню крафтинга
        }else if ("exit_crafting".equals(action)) {
            String previousPage = (String) session.getAttribute("previousPage");
            if (previousPage != null) {
                response.sendRedirect(previousPage);
            } else {
                showInventory(out, inventory, resources);
            }
        } else if ("start_fire".equals(action)) {
            if (inventory.contains("Спички")) {
                out.println("<h1>Вы разводите огонь и проводите ночь в безопасности.</h1>");
                out.println("<p>Однако развести огонь в лесу оказалось непростой задачей и вы потратили все спички</p>");
                inventory.remove("Спички");
                out.println("<p>Ваше здоровье: " + health + "</p>");
            } else {
                out.println("<h1>У вас нет спичек, чтобы развести огонь.</h1>");
                health -= 10;
                session.setAttribute("health", health);
                out.println("<p>Ваше здоровье: " + health + "</p>");
            }
            showInventory(out, inventory, resources);
            // Следующее событие: Поиск воды
            searchWaterEvent(out, session);
        } else if ("find_shelter".equals(action)) {
            if (inventory.contains("Укрытие")) {
                out.println("<h1>Вы находите подходящее место и устанавливаете укрытие.</h1>");
                out.println("<p>Ваше здоровье: " + health + "</p>");
                inventory.remove("Укрытие");
            } else {
                health -= 10;
                session.setAttribute("health", health);
                out.println("<h1>Вы не находите подходящее укрытие и теряете 10 единиц здоровья из-за холода.</h1>");
                out.println("<p>Ваше здоровье: " + health + "</p>");
            }
            showInventory(out, inventory, resources);
            // Следующее событие: Поиск воды
            searchWaterEvent(out, session);
        } else if ("drink_stream".equals(action)) {
            if (inventory.contains("Бутылка воды")) {
                out.println("<h1>Подходя к ручью вы вспоминаете о воде что нашли в самолете и выпиваете ее</h1>");
                out.println("<p>Ваше здоровье: " + health + "</p>");
                inventory.remove("Бутылка воды");
            } else {
            health -= 20;
            session.setAttribute("health", health);
            out.println("<h1>Вы пьете из ручья, но чувствуете недомогание. Ваше здоровье уменьшается на 20 единиц.</h1>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            showInventory(out, inventory, resources);
            // Следующее событие: Встреча с диким животным
            meetAnimalEvent(out, session);
            }
        } else if ("keep_searching".equals(action)) {
                if (inventory.contains("Бутылка воды")) {
                    out.println("<h1>Продолжая путь вы вспоминаете о воде что нашли в самолете и выпиваете ее</h1>");
                    out.println("<p>Ваше здоровье: " + health + "</p>");
                    inventory.remove("Бутылка воды");
                } else {
                    health -= 15;
                    session.setAttribute("health", health);
                    out.println("<h1>Вы продолжаете искать воду, но здоровье ухудшается из-за жажды.</h1>");
                    out.println("<p>Ваше здоровье: " + health + "</p>");
                }
            showInventory(out, inventory, resources);
            // Следующее событие: Встреча с диким животным
            meetAnimalEvent(out, session);
        } else if ("use_weapon".equals(action)) {
            out.println("<h1>Вы используете сигнальный факел и отпугиваете волка.</h1>");
            out.println("К сожалению, факел был единственным оружием, теперь остается расчитывать только на себя");
            inventory.remove("Сигнальный огонь");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            showInventory(out, inventory, resources);
            // Следующее событие: Встреча с выжившим
            meetSurvivorEvent(out, session);
        } else if ("run_away".equals(action)) {
            health -= 25;
            session.setAttribute("health", health);
            out.println("<h1>Вы пытаетесь убежать, но волк ранит вас. Вы теряете 25 единиц здоровья.</h1>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            showInventory(out, inventory, resources);
            // Следующее событие: Встреча с выжившим
            meetSurvivorEvent(out, session);
        } else if ("accept_help".equals(action)) {
            health += 20;
            if (health > 100) health = 100;
            session.setAttribute("health", health);
            out.println("<h1>Вы принимаете помощь и ваше здоровье увеличивается на 20 единиц.</h1>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            showInventory(out, inventory, resources);
            // Завершение квеста
            endQuest(out, session);
        } else if ("decline_help".equals(action)) {
            out.println("<h1>Вы отказываетесь от помощи и продолжаете путь в одиночку.</h1>");
            out.println("<p>Ваше здоровье: " + health + "</p>");
            showInventory(out, inventory, resources);
            // Завершение квеста
            endQuest(out, session);
        } else if ("restart".equals(action)) {
            // Перезапуск квеста
            session.invalidate();
            response.sendRedirect("quest");
        } else {
            // Если действие не распознано, вернуть к предыдущему выбору
            out.println("<h1>Вы не выбрали действие. Пожалуйста, попробуйте еще раз.</h1>");
            // Вернуться к предыдущему состоянию
            response.sendRedirect("quest");
            }

        out.println("</body></html>");
    }


    // Показать инвентарь и ресурсы
    private void showInventory(PrintWriter out, Set<String> inventory, Map<String, Integer> resources) {
        out.println("<h2>Ваш инвентарь:</h2>");
        if (inventory.isEmpty()) {
            out.println("<p>Инвентарь пуст.</p>");
        } else {
            out.println("<ul>");
            for (String item : inventory) {
                out.println("<li>" + item + "</li>");
            }
            out.println("</ul>");
        }

        out.println("<h2>Ваши ресурсы:</h2>");
        if (resources.isEmpty()) {
            out.println("<p>У вас нет ресурсов.</p>");
        } else {
            out.println("<ul>");
            for (Map.Entry<String, Integer> entry : resources.entrySet()) {
                out.println("<li>" + entry.getKey() + ": " + entry.getValue() + "</li>");
            }
            out.println("</ul>");
        }

        // Кнопка крафтинга
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='hidden' name='action' value='craft'>");
        out.println("<input type='submit' value='Создать предмет'>");
        out.println("</form>");
    }

    // Показать доступные для крафта предметы
    private void showCraftingOptions(PrintWriter out, Map<String, Integer> resources, HttpSession session) {
        out.println("<form action='craft_item' method='POST'>");
        out.println("<select name='itemToCraft'>");
        if (resources.getOrDefault("Дерево", 0) >= 2 && resources.getOrDefault("Камень", 0) >= 1) {
            out.println("<option value='Копье'>Копье (2 Дерева, 1 Камень)</option>");
        }
        if (resources.getOrDefault("Дерево", 0) >= 3) {
            out.println("<option value='Укрытие'>Укрытие (3 Дерева)</option>");
        }
        out.println("</select>");

        out.println("<input type='submit' value='Создать предмет'>");
        out.println("</form>");

        String previousPage = (String) session.getAttribute("previousPage");

        if (previousPage != null) {
            out.println("<form action='" + previousPage + "' method='POST'>");
            out.println("<input type='submit' value='Выйти из меню'>");
            out.println("</form>");
        } else {
            out.println("<form action='inventory' method='POST'>");
            out.println("<input type='submit' value='Вернуться в инвентарь'>");
            out.println("</form>");
        }
    }


    // Сбор ресурсов
    private void gatherResources(PrintWriter out, HttpSession session, String resource, int amount) {
        Map<String, Integer> resources = (Map<String, Integer>) session.getAttribute("resources");
        resources.put(resource, resources.getOrDefault(resource, 0) + amount);
        session.setAttribute("resources", resources);
        out.println("<p>Вы находите " + amount + " ед. ресурса: " + resource + ".</p>");
    }

    // Крафтинг предметов
    private String craftItem(Map<String, Integer> resources, String itemToCraft) {
        if ("Копье".equalsIgnoreCase(itemToCraft) && resources.getOrDefault("Дерево", 0) >= 2 && resources.getOrDefault("Камень", 0) >= 1) {
            resources.put("Дерево", resources.get("Дерево") - 2);
            resources.put("Камень", resources.get("Камень") - 1);
            return "Копье";
        } else if ("Укрытие".equalsIgnoreCase(itemToCraft) && resources.getOrDefault("Дерево", 0) >= 3) {
            resources.put("Дерево", resources.get("Дерево") - 3);
            return "Укрытие";
        } else {
            return "Невозможно создать предмет";
        }
    }

    // Событие: Наступление ночи
    private void nightEvent(PrintWriter out, HttpSession session) {
        out.println("<p>Вам нужно найти безопасное место для ночлега.</p>");
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='radio' name='action' value='start_fire'> Развести огонь<br>");
        out.println("<input type='radio' name='action' value='find_shelter'> Найти укрытие<br>");
        out.println("<input type='submit' value='Выбрать'>");
        out.println("</form>");
    }

    // Событие: Поиск воды
    private void searchWaterEvent(PrintWriter out, HttpSession session) {
        out.println("<p>Вас мучает жажда и вы отправляетесь на поиски воды. Спустя время вы находите ручей, но сомневаетесь в его безопасности, что будете делать?</p>");
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='radio' name='action' value='drink_stream'> Выпить из ручья<br>");
        out.println("<input type='radio' name='action' value='keep_searching'> Продолжить искать воду<br>");
        out.println("<input type='submit' value='Выбрать'>");
        out.println("</form>");
    }

    // Событие: Встреча с диким животным
    private void meetAnimalEvent(PrintWriter out, HttpSession session) {
        out.println("<p>Вдруг вы сталкиваетесь с диким волком.</p>");
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='radio' name='action' value='use_weapon'> Использовать сигнальный огонь в надежде отпугнуть животное<br>");
        out.println("<input type='radio' name='action' value='run_away'> Попытаться убежать<br>");
        out.println("<input type='submit' value='Выбрать'>");
        out.println("</form>");
    }

    // Событие: Встреча с выжившим
    private void meetSurvivorEvent(PrintWriter out, HttpSession session) {
        out.println("<p>Вы встречаете другого выжившего, который предлагает вам помощь.</p>");
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='radio' name='action' value='accept_help'> Принять помощь<br>");
        out.println("<input type='radio' name='action' value='decline_help'> Отклонить помощь и продолжить в одиночку<br>");
        out.println("<input type='submit' value='Выбрать'>");
        out.println("</form>");
    }

    // Завершение квеста
    private void endQuest(PrintWriter out, HttpSession session) {
        Integer health = (Integer) session.getAttribute("health");
        if (health > 0) {
            out.println("<h1>Вы находите спасателей и благополучно покидаете лес!</h1>");
            out.println("<p>Конец квеста.</p>");
        } else {
            out.println("<h1>К сожалению, вы не смогли выжить.</h1>");
            out.println("<p>Конец квеста.</p>");
        }
        out.println("<form action='quest' method='POST'>");
        out.println("<input type='submit' name='action' value='restart'> Начать заново");
        out.println("</form>");
        session.invalidate(); // Завершаем сессию после окончания квеста
    }
}
