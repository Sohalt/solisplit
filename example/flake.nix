{
  description = "An example deployment of solisplit using caddy as a reverse proxy";
  #inputs.solisplit.url = "github:sohalt/solisplit"; # <- use this in your standalone flake
  inputs.solisplit.url = ".."; # <- refering to the flake in parent directory
  outputs = {
    nixpkgs,
    solisplit,
    ...
  }: {
    nixosConfigurations.example = nixpkgs.lib.nixosSystem {
      modules = [
        ({config, ...}: {
          # --- genenric setup ---
          # make a container config, so we don't need to think about bootloader, filesystems, etc
          boot.isContainer = true;
          nixpkgs.hostPlatform = "x86_64-linux";

          # --- import flake ---
          # make services.solisplit module available
          imports = [solisplit.nixosModules.solisplit];
          # make solisplit package available
          nixpkgs.overlays = [solisplit.overlays.default];

          # --- configure service ---
          # enable solisplit
          services.solisplit.enable = true;
          # configure reverse proxy
          services.caddy = {
            enable = true;
            virtualHosts = {
              "solisplit.example.com" = {
                extraConfig = "reverse_proxy 127.0.0.1:${toString config.services.solisplit.port}";
              };
            };
          };
        })
      ];
    };
  };
}
