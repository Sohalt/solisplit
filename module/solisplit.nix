{
  config,
  lib,
  pkgs,
  ...
}: let
  cfg = config.services.solisplit;
in
  with lib; {
    options.services.solisplit = {
      enable = mkEnableOption "solisplit";
      port = mkOption {
        type = types.port;
        default = 8000;
        description = "port to listen on";
      };
      server-address = mkOption {
        type = types.str;
        description = "Full address (including scheme) where the service is running.";
        example = "https://solisplit.example.com";
      };
    };
    config = mkIf cfg.enable {
      systemd.services.solisplit = {
        wantedBy = ["multi-user.target"];
        script = "${pkgs.solisplit}/bin/solisplit --port ${toString cfg.port}";
        environment.SERVER_ADDRESS = cfg.server_address;
      };
    };
  }
