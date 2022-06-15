#!/usr/bin/env python3

import argparse
import math
import os
import os.path
import random
import subprocess
import string


AUTHORIZED_KEYS="authorized_keys"
BASE_HW_MAC = "02:0b:2a:11:ff:"
BASE_FAKE_MAC = "00:00:00:00:00:"
BASE_WIREGUARD_IP_RANGE = "10.100.200.1{number}/32"
TEMPLATE_TPX_CONFIG = "config.template.edn"
TEMPLATE_SIP_CONFIG = "sip.template.cfg"
TEMPLATE_WIREGUARD_CONFIG = "wireguard.template.conf"
TEMPLATE_WIREGUARD_SERVER_CONFIG = "wireguard.server.template.conf"
TEMPLATE_MOTD_TELEPORTER = "motd.template.teleporter"
TEMPLATE_MOTD_RPI = "motd.template.rpi"
TEMPLATE_HOSTNAME_TELEPORTER = "hostname.teleporter"
TEMPLATE_HOSTNAME_RPI = "hostname.rpi"
TEMPLATE_ETH1 = "eth1.template"
HOSTNAME_TELEPORTER = "hostname.teleporter"
HOSTNAME_RPI = "hostname.rpi"
MOTD_TELEPORTER = "motd.teleporter"
MOTD_RPI = "motd.rpi"
TPX_CONFIG="config.edn"
SIP_CONFIG="sip.cfg"
ETH1_CONFIG="eth1.teleporter"
WIREGUARD_TELEPORTER_CONFIG="wg0.teleporter.conf"
WIREGUARD_RPI_CONFIG="wg0.rpi.conf"
WIREGUARD_PRIVATE_KEY="wg.private_key.{who}"
WIREGUARD_PUBLIC_KEY="wg.public_key.{who}"
KAMAILIO_COMMANDS="kamailio.commands"
WIREGUARD_SERVER_CONFIG="wireguard.server.config"

def get_random_string(length):
    entries = string.ascii_letters + string.digits
    return ''.join(random.choice(entries) for i in range(length))

def get_number(num):
    return str(num) if num >= 10 else "0" + str(num)

def is_even(num):
    return num % 2 == 0

def get_private_key():
    x = subprocess.run(["wg", "genkey"], stdout=subprocess.PIPE)
    return x.stdout.decode('utf-8').replace('\n', '')

def get_public_key(private_key):
    x = subprocess.run(["wg", "pubkey"], stdout=subprocess.PIPE, input=private_key.encode('utf-8'))
    return x.stdout.decode('utf-8').replace('\n', '')

def get_wireguard_ip(serial_number):
    return "10.100.200." + serial_number + "/32"

def get_ip_teleporter(serial_number):
    return BASE_WIREGUARD_IP_RANGE.format(number=get_number(serial_number))

def get_ip_rpi(serial_number):
    return BASE_WIREGUARD_IP_RANGE.format(number=get_number(serial_number))

def get_sip_id(serial_number):
    serial = get_number(serial_number)
    return "91{id}".format(id=serial)

def write_config(path, name, config):
    with open(os.path.join(path, name), 'w') as f:
        f.write(config)

def read_config(path, name):
    out = None
    with open(os.path.join(path, name)) as x:
        out = x.read()
    return out

def list_p(x):
    return type(x) == list

def copy_file(source, dest):
    s = os.path.join(*source) if list_p(source) else source
    d = os.path.join(*dest) if list_p(dest) else dest
    os.system("cp {source} {dest}".format(source=s, dest=d))

def write_sip_config(template_path, config_path, serial_number):
    sip_user = get_sip_id(serial_number)
    sip_password = get_random_string(32)

    sip_config = read_config(template_path, TEMPLATE_SIP_CONFIG)

    sip_config = sip_config.replace("VAR__SIP_USER", sip_user)
    sip_config = sip_config.replace("VAR__SIP_PASSWORD", sip_password)

    write_config(config_path, SIP_CONFIG, sip_config)

    kamailio_commands = ""
    kamailio_commands += "kamctl add {user} {password}\n".format(user=sip_user, password=sip_password)
    write_config(config_path, KAMAILIO_COMMANDS, kamailio_commands)

def write_tpx_config(template_path, config_path, name, serial_number, logger_level):
    serial = get_number(serial_number)

    tpx_config = read_config(template_path, TEMPLATE_TPX_CONFIG)

    tpx_config = tpx_config.replace("VAR__TELEPORTER_NICKNAME", "{name} 91{serial}".format(name=name, serial=serial))
    tpx_config = tpx_config.replace("VAR__MQTT_USERNAME", "songpark")
    tpx_config = tpx_config.replace("VAR__MQTT_PASSWORD", "fNhWktaTlfDGlH4mbmaW6esOpgExs8wKIOBapDcq")
    tpx_config = tpx_config.replace("VAR__FAKE_MAC", BASE_FAKE_MAC + get_number(serial_number/2))
    tpx_config = tpx_config.replace("VAR__HW_MAC", BASE_HW_MAC + get_number(serial_number))
    tpx_config = tpx_config.replace("VAR__LOGGER_LEVEL", ":" + logger_level)

    write_config(config_path, TPX_CONFIG, tpx_config)

def write_wireguard_config(template_path, config_path, name, serial_number):
    private_key_teleporter = get_private_key()
    public_key_teleporter = get_public_key(private_key_teleporter)
    ip_teleporter = get_ip_teleporter(serial_number)

    private_key_rpi = get_private_key()
    public_key_rpi = get_public_key(private_key_rpi)
    ip_rpi = get_ip_rpi(serial_number)

    wireguard_teleporter_config = read_config(template_path, TEMPLATE_WIREGUARD_CONFIG)
    wireguard_teleporter_config = wireguard_teleporter_config.replace("VAR__WIREGUARD_PRIVATE_KEY", private_key_teleporter)
    wireguard_teleporter_config = wireguard_teleporter_config.replace("VAR__WIREGUARD_IP", ip_teleporter)

    wireguard_rpi_config = read_config(template_path, TEMPLATE_WIREGUARD_CONFIG)
    wireguard_rpi_config = wireguard_rpi_config.replace("VAR__WIREGUARD_PRIVATE_KEY", private_key_rpi)
    wireguard_rpi_config = wireguard_rpi_config.replace("VAR__WIREGUARD_IP", ip_rpi)

    wireguard_server_config = read_config(template_path, TEMPLATE_WIREGUARD_SERVER_CONFIG)
    wireguard_server_config = wireguard_server_config.replace("VAR__PUBLIC_KEY_TELEPORTER", public_key_teleporter)
    wireguard_server_config = wireguard_server_config.replace("VAR__IP_TELEPORTER", ip_teleporter)
    wireguard_server_config = wireguard_server_config.replace("VAR__PUBLIC_KEY_RPI", public_key_rpi)
    wireguard_server_config = wireguard_server_config.replace("VAR__IP_RPI", ip_rpi)
    wireguard_server_config = wireguard_server_config.replace("VAR__NAME", name)

    write_config(config_path, WIREGUARD_TELEPORTER_CONFIG, wireguard_teleporter_config)
    write_config(config_path, WIREGUARD_RPI_CONFIG, wireguard_rpi_config)
    write_config(config_path, WIREGUARD_SERVER_CONFIG, wireguard_server_config)

    write_config(config_path, WIREGUARD_PRIVATE_KEY.format(who="teleporter"), private_key_teleporter)
    write_config(config_path, WIREGUARD_PRIVATE_KEY.format(who="rpi"), private_key_rpi)
    write_config(config_path, WIREGUARD_PUBLIC_KEY.format(who="teleporter"), public_key_teleporter)
    write_config(config_path, WIREGUARD_PUBLIC_KEY.format(who="rpi"), public_key_rpi)
    copy_file([template_path, AUTHORIZED_KEYS],
              [config_path, AUTHORIZED_KEYS])

def write_motd(template_path, config_path, name, serial_number):
    motd_teleporter = read_config(template_path, TEMPLATE_MOTD_TELEPORTER)
    motd_rpi = read_config(template_path, TEMPLATE_MOTD_RPI)

    ip_rpi = get_ip_rpi(serial_number)
    sip_id = get_sip_id(serial_number)

    motd_teleporter = motd_teleporter.replace("VAR__NAME", name)
    motd_teleporter = motd_teleporter.replace("VAR__IP_RPI", ip_rpi)
    motd_teleporter = motd_teleporter.replace("VAR__SIP_ID", sip_id)

    motd_rpi = motd_rpi.replace("VAR__NAME", name)

    write_config(config_path, MOTD_TELEPORTER, motd_teleporter)
    write_config(config_path, MOTD_RPI, motd_rpi)

def write_hostname(template_path, config_path, name):
    hostname_teleporter = read_config(template_path, TEMPLATE_HOSTNAME_TELEPORTER)
    hostname_rpi = read_config(template_path, TEMPLATE_HOSTNAME_RPI)

    hostname_teleporter = hostname_teleporter.replace("VAR__NAME", name)
    hostname_rpi = hostname_rpi.replace("VAR__NAME", name)

    write_config(config_path, HOSTNAME_TELEPORTER, hostname_teleporter)
    write_config(config_path, HOSTNAME_RPI, hostname_rpi)

def write_eth1_config(template_path, config_path, serial_number):
    hw_mac = BASE_HW_MAC + get_number(serial_number)
    eth1_config = read_config(template_path, TEMPLATE_ETH1)

    eth1_config = eth1_config.replace("VAR__HW_MAC", hw_mac)

    write_config(config_path, ETH1_CONFIG, eth1_config)

def generate_configs(template_path, config_path, logger_level, name, serial_number):
    os.system("mkdir -p " + config_path)

    if not is_even(serial_number):
        print("Serial number is not even. Use even numbers")
        exit(0)

    write_sip_config(template_path, config_path, serial_number)
    write_tpx_config(template_path, config_path, name, serial_number, logger_level)
    write_eth1_config(template_path, config_path, serial_number)
    write_wireguard_config(template_path, config_path, name, serial_number)
    write_motd(template_path, config_path, name, serial_number)
    write_hostname(template_path, config_path, name)

def copy_configs_to_teleporter(config_path, songpark_path, root_path):
    copy_file([config_path, AUTHORIZED_KEYS],
              [root_path, "root/.ssh", AUTHORIZED_KEYS])
    copy_file([config_path, WIREGUARD_TELEPORTER_CONFIG],
              [root_path, "etc/wireguard", "wg0.conf"])
    copy_file([config_path, WIREGUARD_PUBLIC_KEY.format(who="teleporter")],
              [root_path, "etc/wireguard/keys", "publickey"])
    copy_file([config_path, WIREGUARD_PRIVATE_KEY.format(who="teleporter")],
              [root_path, "etc/wireguard/keys", "privatekey"])
    copy_file([config_path, TPX_CONFIG],
              [songpark_path, "usr/local/etc", TPX_CONFIG])
    copy_file([config_path, SIP_CONFIG],
              [songpark_path, "usr/local/etc", SIP_CONFIG])
    copy_file([config_path, MOTD_TELEPORTER],
              [root_path, "/etc", "motd.txt"])
    copy_file([config_path, HOSTNAME_TELEPORTER],
              [root_path, "/etc", "hostname"])

def copy_configs_to_rpi(config_path, rpi_path):
    copy_file([config_path, AUTHORIZED_KEYS],
              [rpi_path, "home/pi/.ssh", AUTHORIZED_KEYS])
    copy_file([config_path, WIREGUARD_RPI_CONFIG],
              [rpi_path, "etc/wireguard", "wg0.conf"])
    copy_file([config_path, WIREGUARD_PUBLIC_KEY.format(who="rpi")],
              [rpi_path, "etc/wireguard/keys", "publickey"])
    copy_file([config_path, WIREGUARD_PRIVATE_KEY.format(who="rpi")],
              [rpi_path, "etc/wireguard/keys", "privatekey"])
    copy_file([config_path, MOTD_RPI],
              [rpi_path, "/etc", "motd.txt"])
    copy_file([config_path, HOSTNAME_RPI],
              [root_path, "/etc", "hostname"])

def cleanup_configs(config_path):
    os.system("rm -rf " + config_path)

def run_main():
    try:
        parser = argparse.ArgumentParser()
        parser.add_argument("--teleporter-name", help="What is the Teleporter to be known as?")
        parser.add_argument("--teleporter-serial", help="Which serial is the Teleporter. Only even numbers", type=int)
        parser.add_argument("--template-path", help="Path to read template configuration files from", default=None)
        parser.add_argument('--config-path', help='Path to write configuration files to', default='config-files')
        parser.add_argument("--logger-level", help="Logger level for logging in TPX", default="info")
        parser.add_argument("--songpark-path", help="Path to the songpark partition of the Teleporter SD card image")
        parser.add_argument("--root-path", help="Path to the root partition of the Teleporter SD card image")
        parser.add_argument("--rpi-path", help="Path to the RPi SD card image")
        parser.add_argument("--action",
                            help="Which action to perform",
                            choices=["generate-configs",
                                     "copy-configs-to-teleporter",
                                     "copy-configs-to-rpi",
                                     "clear-configs"])
        args = parser.parse_args()

        if args.action == "generate-configs":
            assert (args.template_path is not None), "--template-path must be provided"
            assert (args.config_path is not None), "--config-path must be provided"
            assert (args.teleporter_name is not None), "--teleporter-name must be provided"
            assert (args.teleporter_serial is not None), "--teleporter-serial must be provided"
            generate_configs(args.template_path, args.config_path, args.logger_level, args.teleporter_name, args.teleporter_serial)
        elif args.action == "copy-configs-to-teleporter":
            assert (args.config_path is not None), "--config-path must be provided"
            assert (args.songpark_path is not None), "--songpark-path must be provided"
            assert (args.root_path is not None), "--root-path must be provided"

            copy_configs_to_teleporter(args.config_path, args.songpark_path, args.root_path)
        elif args.action == "copy-configs-to-rpi":
            assert (args.config_path is not None), "--config-path must be provided"
            assert (args.rpi_path is not None), "--rpi-path must be provided"

            copy_configs_to_rpi(args.config_path, args.rpi_path)
        elif args.action == "clear-configs":
            assert (args.config_path is not None), "--config-path must be provided"
            cleanup_configs(args.config_path)
        else:
            print("--action missing")
            print("You supplied the following args")
            print(args)
    except Exception as e:
        print(e)


if __name__ == "__main__":
    run_main()
