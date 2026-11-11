#include <a_samp>

#define VORTEX_DB_FILE       "vortex_vip.db"
#define PAYDAY_INTERVAL     (60 * 60 * 1000) // 60 минут
#define PAYDAY_BASE_SALARY  (1000)
#define VIP_DURATION        (30 * 24 * 60 * 60) // 30 дней

#define VIP_BRONZE_PRICE    (500)
#define VIP_SILVER_PRICE    (1200)
#define VIP_GOLD_PRICE      (2500)

#define VIP_BRONZE_REWARD   (2)
#define VIP_SILVER_REWARD   (5)
#define VIP_GOLD_REWARD     (10)

#define DIALOG_VIP          (4100)

new DB:gVortexDB;
new gPlayerVC[MAX_PLAYERS];
new gPlayerVIP[MAX_PLAYERS];
new gPlayerVIPUntil[MAX_PLAYERS];

forward VortexPayday();

stock EscapeSQL(const input[], output[], outputSize)
{
    new j;
    for (new i = 0; input[i] != EOS && j < outputSize - 2; i++)
    {
        output[j++] = input[i];
        if (input[i] == 39) output[j++] = 39; // SQL escape: ' -> ''
    }
    output[j] = EOS;
    return 1;
}

stock SaveVortexAccount(playerid)
{
    if (!IsPlayerConnected(playerid) || gVortexDB == DB:0) return 0;

    new name[MAX_PLAYER_NAME + 1], escaped[MAX_PLAYER_NAME * 2 + 1], query[256];
    GetPlayerName(playerid, name, sizeof name);
    EscapeSQL(name, escaped, sizeof escaped);

    format(query, sizeof query,
        "UPDATE vortex_accounts SET vc=%d, vip_level=%d, vip_until=%d WHERE player_name='%s'",
        gPlayerVC[playerid], gPlayerVIP[playerid], gPlayerVIPUntil[playerid], escaped);
    db_free_result(db_query(gVortexDB, query));
    return 1;
}

stock LoadVortexAccount(playerid)
{
    new name[MAX_PLAYER_NAME + 1], escaped[MAX_PLAYER_NAME * 2 + 1], query[256];
    GetPlayerName(playerid, name, sizeof name);
    EscapeSQL(name, escaped, sizeof escaped);

    format(query, sizeof query,
        "INSERT OR IGNORE INTO vortex_accounts (player_name, vc, vip_level, vip_until) VALUES ('%s', 0, 0, 0)",
        escaped);
    db_free_result(db_query(gVortexDB, query));

    format(query, sizeof query,
        "SELECT vc, vip_level, vip_until FROM vortex_accounts WHERE player_name='%s' LIMIT 1",
        escaped);
    new DBResult:result = db_query(gVortexDB, query);

    gPlayerVC[playerid] = 0;
    gPlayerVIP[playerid] = 0;
    gPlayerVIPUntil[playerid] = 0;

    if (db_num_rows(result))
    {
        gPlayerVC[playerid] = db_get_field_assoc_int(result, "vc");
        gPlayerVIP[playerid] = db_get_field_assoc_int(result, "vip_level");
        gPlayerVIPUntil[playerid] = db_get_field_assoc_int(result, "vip_until");
    }
    db_free_result(result);

    if (gPlayerVIPUntil[playerid] <= gettime())
    {
        gPlayerVIP[playerid] = 0;
        gPlayerVIPUntil[playerid] = 0;
        SaveVortexAccount(playerid);
    }
    return 1;
}

stock GetNextToken(const source[], &index, destination[], destinationSize)
{
    while (source[index] == ' ') index++;
    new length;
    while (source[index] != EOS && source[index] != ' ' && length < destinationSize - 1)
        destination[length++] = source[index++];
    destination[length] = EOS;
    return length;
}

stock GetVIPPrice(level)
{
    switch (level)
    {
        case 1: return VIP_BRONZE_PRICE;
        case 2: return VIP_SILVER_PRICE;
        case 3: return VIP_GOLD_PRICE;
    }
    return 0;
}

stock GetVIPReward(level)
{
    switch (level)
    {
        case 1: return VIP_BRONZE_REWARD;
        case 2: return VIP_SILVER_REWARD;
        case 3: return VIP_GOLD_REWARD;
    }
    return 0;
}

stock GetVIPName(level, output[], outputSize)
{
    switch (level)
    {
        case 1: format(output, outputSize, "Bronze VIP");
        case 2: format(output, outputSize, "Silver VIP");
        case 3: format(output, outputSize, "Gold VIP");
        default: format(output, outputSize, "Нет VIP");
    }
    return 1;
}

public OnGameModeInit()
{
    SetGameModeText("Vortex Role Play | VIP & Payday");
    AddPlayerClass(0, 1481.0, -1771.0, 18.8, 0.0, 0, 0, 0, 0, 0, 0);

    gVortexDB = db_open(VORTEX_DB_FILE);
    if (gVortexDB == DB:0)
    {
        print("[VORTEX] Не удалось открыть SQLite базу данных.");
        SendRconCommand("exit");
        return 1;
    }

    db_free_result(db_query(gVortexDB,
        "CREATE TABLE IF NOT EXISTS vortex_accounts (player_name TEXT PRIMARY KEY, vc INTEGER NOT NULL DEFAULT 0, vip_level INTEGER NOT NULL DEFAULT 0, vip_until INTEGER NOT NULL DEFAULT 0)"));

    SetTimer("VortexPayday", PAYDAY_INTERVAL, true);
    print("[VORTEX] VIP и Payday загружены. Payday: 60 минут.");
    return 1;
}

public OnGameModeExit()
{
    for (new i = 0; i < MAX_PLAYERS; i++)
    {
        if (IsPlayerConnected(i)) SaveVortexAccount(i);
    }
    if (gVortexDB != DB:0) db_close(gVortexDB);
    return 1;
}

public OnPlayerConnect(playerid)
{
    LoadVortexAccount(playerid);
    SendClientMessage(playerid, 0xA78BFAFF, "[VORTEX] Добро пожаловать! /vip — VIP-магазин, /vc — баланс VC.");
    return 1;
}

public OnPlayerDisconnect(playerid, reason)
{
    SaveVortexAccount(playerid);
    gPlayerVC[playerid] = 0;
    gPlayerVIP[playerid] = 0;
    gPlayerVIPUntil[playerid] = 0;
    return 1;
}

public VortexPayday()
{
    new message[144], vipName[24];
    for (new playerid = 0; playerid < MAX_PLAYERS; playerid++)
    {
        if (!IsPlayerConnected(playerid)) continue;

        GivePlayerMoney(playerid, PAYDAY_BASE_SALARY);

        if (gPlayerVIP[playerid] > 0 && gPlayerVIPUntil[playerid] > gettime())
        {
            new reward = GetVIPReward(gPlayerVIP[playerid]);
            gPlayerVC[playerid] += reward;
            GetVIPName(gPlayerVIP[playerid], vipName, sizeof vipName);
            format(message, sizeof message,
                "[PAYDAY] Зарплата: $%d. %s начислил +%d VC. Баланс: %d VC.",
                PAYDAY_BASE_SALARY, vipName, reward, gPlayerVC[playerid]);
        }
        else
        {
            if (gPlayerVIP[playerid] > 0)
            {
                gPlayerVIP[playerid] = 0;
                gPlayerVIPUntil[playerid] = 0;
            }
            format(message, sizeof message,
                "[PAYDAY] Зарплата: $%d. VIP нет — бонус VC не начислен.",
                PAYDAY_BASE_SALARY);
        }

        SendClientMessage(playerid, 0xA78BFAFF, message);
        SaveVortexAccount(playerid);
    }
    print("[VORTEX] Payday выполнен.");
    return 1;
}

public OnPlayerCommandText(playerid, cmdtext[])
{
    if (!strcmp(cmdtext, "/vc", true))
    {
        new message[96];
        format(message, sizeof message, "[VORTEX] Твой баланс: %d VC.", gPlayerVC[playerid]);
        SendClientMessage(playerid, 0xA78BFAFF, message);
        return 1;
    }

    if (!strcmp(cmdtext, "/vcgive", true, 7) && cmdtext[7] == ' ')
    {
        if (!IsPlayerAdmin(playerid))
        {
            SendClientMessage(playerid, 0xFF7777FF, "[VORTEX] Команда доступна только RCON-администрации.");
            return 1;
        }

        new index = 8, targetText[12], amountText[16];
        GetNextToken(cmdtext, index, targetText, sizeof targetText);
        GetNextToken(cmdtext, index, amountText, sizeof amountText);

        new targetid = strval(targetText);
        new amount = strval(amountText);
        if (targetText[0] == EOS || amountText[0] == EOS || targetid < 0 || targetid >= MAX_PLAYERS || !IsPlayerConnected(targetid) || amount <= 0 || amount > 1000000)
        {
            SendClientMessage(playerid, 0xFF7777FF, "Использование: /vcgive [playerid] [количество 1-1000000]");
            return 1;
        }

        gPlayerVC[targetid] += amount;
        SaveVortexAccount(targetid);

        new message[112];
        format(message, sizeof message, "[VORTEX] Администратор начислил тебе %d VC. Баланс: %d VC.", amount, gPlayerVC[targetid]);
        SendClientMessage(targetid, 0xA78BFAFF, message);
        SendClientMessage(playerid, 0xA78BFAFF, "[VORTEX] VC успешно начислены.");
        return 1;
    }

    if (!strcmp(cmdtext, "/vip", true))
    {
        new info[512];
        format(info, sizeof info,
            "Bronze VIP — %d VC | +%d VC за Payday\nSilver VIP — %d VC | +%d VC за Payday\nGold VIP — %d VC | +%d VC за Payday",
            VIP_BRONZE_PRICE, VIP_BRONZE_REWARD,
            VIP_SILVER_PRICE, VIP_SILVER_REWARD,
            VIP_GOLD_PRICE, VIP_GOLD_REWARD);
        ShowPlayerDialog(playerid, DIALOG_VIP, DIALOG_STYLE_LIST,
            "VORTEX | VIP-магазин", info, "Купить", "Закрыть");
        return 1;
    }
    return 0;
}

public OnDialogResponse(playerid, dialogid, response, listitem, inputtext[])
{
    if (dialogid != DIALOG_VIP) return 0;
    if (!response) return 1;

    new level = listitem + 1;
    new price = GetVIPPrice(level);
    new reward = GetVIPReward(level);
    if (price <= 0) return 1;

    if (gPlayerVIP[playerid] > level && gPlayerVIPUntil[playerid] > gettime())
    {
        SendClientMessage(playerid, 0xFF7777FF, "[VORTEX] У тебя уже действует VIP более высокого уровня.");
        return 1;
    }
    if (gPlayerVC[playerid] < price)
    {
        new message[96];
        format(message, sizeof message, "[VORTEX] Не хватает VC. Цена: %d VC, баланс: %d VC.", price, gPlayerVC[playerid]);
        SendClientMessage(playerid, 0xFF7777FF, message);
        return 1;
    }

    gPlayerVC[playerid] -= price;
    if (gPlayerVIP[playerid] == level && gPlayerVIPUntil[playerid] > gettime())
        gPlayerVIPUntil[playerid] += VIP_DURATION;
    else
        gPlayerVIPUntil[playerid] = gettime() + VIP_DURATION;

    gPlayerVIP[playerid] = level;
    SaveVortexAccount(playerid);

    new vipName[24], message[144];
    GetVIPName(level, vipName, sizeof vipName);
    format(message, sizeof message,
        "[VORTEX] Куплен %s на 30 дней! За каждый Payday начисляется +%d VC. Остаток: %d VC.",
        vipName, reward, gPlayerVC[playerid]);
    SendClientMessage(playerid, 0xA78BFAFF, message);
    return 1;
}
